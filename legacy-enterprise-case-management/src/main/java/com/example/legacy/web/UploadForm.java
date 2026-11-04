package com.example.legacy.web;
import org.apache.struts.action.ActionForm;
import org.apache.struts.upload.FormFile;
public final class UploadForm extends ActionForm {
 private static final long serialVersionUID=1L;
 private String id;private String csrf;private FormFile file;
 public String getId(){return id;}public void setId(String id){this.id=id;}
 public String getCsrf(){return csrf;}public void setCsrf(String csrf){this.csrf=csrf;}
 public FormFile getFile(){return file;}public void setFile(FormFile file){this.file=file;}
}
