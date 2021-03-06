package org.yxyqcy.family.app.code.service.impl;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.yxyqcy.family.app.code.model.CodeSchema;
import org.yxyqcy.family.app.code.model.CodeTable;
import org.yxyqcy.family.app.code.service.CodeService;
import org.yxyqcy.family.common.config.Global;
import org.yxyqcy.family.common.model.DbColumnModel;
import org.yxyqcy.family.common.model.DbTableModel;
import org.yxyqcy.family.common.service.BaseService;
import org.yxyqcy.family.common.util.DateUtil;
import org.yxyqcy.family.common.util.ZipCompress;
import org.yxyqcy.family.sys.component.FamilyDbUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by lcy on 17/6/28.
 */
@Service
@Transactional(readOnly = true,propagation = Propagation.NOT_SUPPORTED)
public class CodeServiceImpl extends BaseService implements CodeService {

    private static final long serialVersionUID = -3827321264317955429L;
    @Autowired
    private FamilyDbUtils familyDbUtils;

    @Autowired
    private Configuration freeMarkerConfiguration;



    public List<CodeTable> getCodeTablesBySchema(CodeSchema schema){
        List<CodeTable> tables = new ArrayList<>();
        Connection conn  = familyDbUtils.getConnection(schema);;
        ResultSet rs = null;
        DatabaseMetaData dbmd = null;
        CodeTable ct = null;

        try {
            dbmd = (DatabaseMetaData) conn.getMetaData();
            rs=(ResultSet) dbmd.getTables(null, schema.getDbName(), "%", null);
            while(rs.next()){
                ct = new CodeTable();
                ct.setTableName(rs.getString("TABLE_NAME"));
                tables.add(ct);
            }
        }catch (Exception e){
            logger.error(e.getMessage());
        }finally {
            this.killConnection(rs,conn);
        }
        return tables;
    }

    @Override
    public String generateCode(CodeSchema codeSchema) {
        String filePath = null;
        if(null == codeSchema.getTables() && "".equals(codeSchema.getTables().trim()))
            return null;
        List<DbTableModel> tableModels = new ArrayList<DbTableModel>();
        List<DbColumnModel> models = null;
        String[] tables = codeSchema.getTables().split(",");
        Connection conn  = familyDbUtils.getConnection(codeSchema);;
        ResultSet rs = null;
        DatabaseMetaData dbmd = null;
        CodeTable ct = null;
        DbTableModel tableModel = null;
        DbColumnModel model = null;
        try {
            dbmd = (DatabaseMetaData) conn.getMetaData();
            for(String table : tables) {
                // table
                tableModel = new DbTableModel();
                models = new ArrayList<DbColumnModel>();
                tableModel.setTableName(table.toLowerCase());
                //column
                rs = (ResultSet) dbmd.getColumns(null, codeSchema.getDbName(), table.toLowerCase(), null);
                dbmd.getPrimaryKeys(null, null, tables[0].toLowerCase());
                while (rs.next()) {
                    model = new DbColumnModel();
                    model.copyColumnFromSqlResult(rs);
                    models.add(model);
                }
                rs = (ResultSet) dbmd.getPrimaryKeys(null, codeSchema.getDbName(), table.toLowerCase());
                while (rs.next()) {
                    String column = rs.getString("COLUMN_NAME");
                    for(DbColumnModel cm : models){
                        if(column.equals(cm.getColName())){
                            cm.setKey(true);
                            break;
                        }
                    }
                }
                //table -> column
                tableModel.setColumnModels(models);
                tableModels.add(tableModel);
            }

            filePath = this.productCodeFromTable(codeSchema.getAuthor(),codeSchema.getUserId(),codeSchema.getBasePackage(),tableModels);

        }catch (Exception e){
            logger.error(e.getMessage());
        }finally {
            this.killConnection(rs,conn);
        }
        return filePath;
    }

    /**
     * 通过freemarker 成功多层代码
     * @param tableModels
     * @return
     */
    private String productCodeFromTable(String author,String userId,String packageName,List<DbTableModel> tableModels) {

        Map root = null;
        List<File> fileList = new ArrayList<File>();
        File file = null;
        String dirPath = Global.getConfig("base_upload_location")
                + Global.getConfig("code_generate_location") +
                File.separator + userId +
                File.separator + DateUtil.getSimepleDate("yyyyMMddHHmmss", new Date());
        String zipPath = dirPath + ".zip";
        String zipDirPath = dirPath;
        //将. 换/
        String pathPattern = packageName.replaceAll("\\.","\\" +File.separator);
        //存在删除
        dirPath = dirPath + File.separator + pathPattern;  //!! 此行一定要在 zipPath 生成后
        File dirF = new File(dirPath);
        File zipFile = new File(zipPath);
        dirF.deleteOnExit();
        zipFile.deleteOnExit();
        //生成
        try {
            for(DbTableModel dbTableModel : tableModels) {
                root = new HashMap<String,Object>();
                String modelNameUpperCamel = dbTableModel.getEntityName();
                String basePackage = packageName +"."+modelNameUpperCamel.toLowerCase();
                root.put("author", author);
                root.put("table", dbTableModel);
                root.put("basePackage", basePackage);
                //通过一个文件输出流，就可以写到相应的文件中
                //1.entity , mapper
                String newPath = dirPath+File.separator+modelNameUpperCamel.toLowerCase();
                file = this.generateEntityModel(newPath,dbTableModel,root,"Entity");
                fileList.add(file);

                file = this.generateEntityModel(newPath,dbTableModel,root,"Mapper");
                fileList.add(file);
                //2.service
                file = this.generateEntityModel(newPath,dbTableModel,root,"Service");
                fileList.add(file);
                //3.serviceImpl
                file = this.generateEntityModel(newPath,dbTableModel,root,"ServiceImpl");
                fileList.add(file);
                //4.dao
                file = this.generateEntityModel(newPath,dbTableModel,root,"Repository");
                fileList.add(file);
                //5.controller
                file = this.generateEntityModel(newPath,dbTableModel,root,"Controller");
                fileList.add(file);
                //6.list jsp

                //7.list js

                //8.add jsp

                //9.add js

            }
            ZipCompress.compressExe(zipDirPath,zipPath);
        } catch (Exception e) {
            zipPath = null;
            e.printStackTrace();
        }
        return zipPath;
    }

    /**
     * 关闭 connection
     * @param rs
     * @param conn
     */
    private void killConnection(ResultSet rs,Connection conn){
        try {
            if(null != rs)
                rs.close();
            if(null != conn)
                conn.close();
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * model  构建  entity 层
     * @param dirPath
     * @param dbTableModel
     * @param root
     * @return
     */
    private File generateEntityModel(String dirPath,DbTableModel dbTableModel,Map root,String suffix){
        //源文件夹
        File dirFile = null;
        //目标文件
        File file = null;
        FileWriter out = null;
        Template temp = null;
        if(suffix.equals("Repository"))
            dirFile = new File(dirPath + File.separator + "dao");
        else if(suffix.equals("ServiceImpl"))
            dirFile = new File(dirPath + File.separator + "service"+File.separator+"impl");
        else
            dirFile = new File(dirPath + File.separator + suffix.toLowerCase());
        //不存在 创建文件夹
        if(!dirFile.exists())
            dirFile.mkdirs();
        if(suffix.equals("Repository"))
            file = new File(dirPath  + File.separator +  "dao" +
                    File.separator +   dbTableModel.getEntityName() + suffix+".java");
        else if(suffix.equals("ServiceImpl"))
            file = new File(dirPath  + File.separator +  "service"+File.separator+"impl" +
                    File.separator +   dbTableModel.getEntityName() +suffix+".java");
        else if(suffix.equals("Entity"))
            file = new File(dirPath  + File.separator +  suffix.toLowerCase() +
                    File.separator +   dbTableModel.getEntityName() +".java");
        else if(suffix.equals("Mapper"))
            file = new File(dirPath  + File.separator +  suffix.toLowerCase() +
                    File.separator +   dbTableModel.getEntityName() +suffix+".xml");
        else
            file = new File(dirPath  + File.separator +  suffix.toLowerCase() +
                    File.separator +   dbTableModel.getEntityName() +suffix+".java");
        try {
            out = new FileWriter(file);
            //fileList.add(file);
            temp = freeMarkerConfiguration.getTemplate(suffix.toLowerCase()+".ftl");
            temp.process(root, out);
        }catch (Exception e){
            logger.error(e.getMessage());
        }finally {
            try {
                if(out!=null)
                    out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return file;
    }

}
