package com.seitenbau.jenkins.plugins.dynamicparameter;

import hudson.Extension;
import hudson.Util;
import hudson.model.ManagementLink;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/** Plugin configuration. */
@Extension
public class DynamicParameterManagement extends ManagementLink
{
  private static final String[] EMPTY_ARRAY = new String[0];

  @Override
  public String getIconFileName()
  {
    return "notepad.png";
  }

  @Override
  public String getUrlName()
  {
    return "dynamicparameter";
  }

  @Override
  public String getDisplayName()
  {
    return "Dynamic Parameter";
  }

  @Override
  public String getDescription()
  {
    return "Settings for dynamic parameters";
  }

  /**
   * Get the class path directory.
   * @return path to the class path directory
   */
  public String getClassPathDir()
  {
    return DynamicParameterConfiguration.INSTANCE.getClassPathDirectory();
  }

  /**
   * Get a list with uploaded files.
   * @return an array with file names or an empty array
   */
  public String[] getUploadedFiles()
  {
    File classPathDir = new File(getClassPathDir());
    if (classPathDir.isDirectory())
    {
      return classPathDir.list();
    }
    return EMPTY_ARRAY;
  }

  /**
   * Set the class path directory.
   * @param classPathDir new class path directory
   * @return redirect to {@code index}
   * @throws IOException
   * @throws ServletException
   */
  public HttpResponse doSetClassPathDir(@QueryParameter("classPathDir") String classPathDir)
      throws IOException, ServletException
  {
    DynamicParameterConfiguration.INSTANCE.setClassPathDirectory(classPathDir);
    return new HttpRedirect("index");
  }

  /**
   * Upload a script file to the class path directory.
   * @param request HTTP request
   * @return redirect to {@code index}
   * @throws Exception
   */
  public HttpResponse doUploadFile(StaplerRequest request) throws Exception
  {
    File directory = new File(getClassPathDir());
    if (!directory.exists())
    {
      directory.mkdirs();
    }

    FileItem fileItem = request.getFileItem("file");
    String fileName = Util.getFileName(fileItem.getName());
    if (StringUtils.isEmpty(fileName))
    {
      return new HttpRedirect(".");
    }
    fileItem.write(new File(directory, fileName));
    return new HttpRedirect("index");
  }

  /**
   * Download the specified file.
   * @param request HTTP request
   * @param response HTTP response
   * @param filePath file path
   * @throws ServletException
   * @throws IOException
   */
  public void doDownloadFile(StaplerRequest request, StaplerResponse response,
      @QueryParameter("file") String filePath) throws ServletException, IOException
  {
    File file = new File(getClassPathDir(), filePath);
    FileInputStream inputStream = new FileInputStream(file);
    try
    {
      response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
      response.serveFile(request, inputStream, file.lastModified(), file.length(), file.getName());
    }
    finally
    {
      inputStream.close();
    }
  }

  /**
   * Delete the specified file.
   * @param filePath file path
   * @return
   */
  public HttpResponse doDeleteFile(@QueryParameter("file") String filePath)
  {
    File file = new File(getClassPathDir(), filePath);
    if(file.isFile())
    {
      file.delete();
    }
    return new HttpRedirect("index");
  }
}
