package com.seitenbau.jenkins.plugins.dynamicparameter.config;

import hudson.Extension;
import hudson.Util;
import hudson.model.ManagementLink;
import hudson.model.Hudson;
import hudson.security.Permission;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.seitenbau.jenkins.plugins.dynamicparameter.util.FileUtils;

/** Plugin configuration. */
@Extension
public class DynamicParameterManagement extends ManagementLink
{
  private static final String DEFAULT_NAME = "_newClassPath";

  private String lastDirectory = "";

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

  // read methods

  /**
   * Get the class path directory.
   * @return path to the class path directory
   */
  public static String getBaseDirectory() throws IOException
  {
    checkReadPermission();

    return getBaseDirectoryPath();
  }

  /**
   * Get the last used class path directory.
   * @return last used directory or an empty string
   */
  public String getLastClassPathDirectory()
  {
    checkReadPermission();

    return lastDirectory;
  }

  /**
   * Get a list of all class path directories.
   * @return a list of directory names or an empty array
   */
  public String[] getClassPathDirectories() throws IOException
  {
    checkReadPermission();

    File classPathDir = new File(getBaseDirectoryPath());
    if (classPathDir.isDirectory())
    {
      return classPathDir.list();
    }
    return ArrayUtils.EMPTY_STRING_ARRAY;
  }

  /**
   * Get a list with uploaded files.
   * @param classPath class path directory to list
   * @return an array with file names or an empty array
   */
  public String[] getUploadedFiles(String classPath) throws IOException
  {
    checkReadPermission();

    File classPathDir = getRebasedFile(classPath);
    if (classPathDir.isDirectory())
    {
      return classPathDir.list();
    }
    return ArrayUtils.EMPTY_STRING_ARRAY;
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
    checkReadPermission();

    File file = getRebasedFile(filePath);
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

  // write methods

  /**
   * Set the base directory.
   * @param baseDirectory new base directory
   * @return redirect to {@code index}
   * @throws IOException
   * @throws ServletException
   */
  public HttpResponse doSetClassPathDir(@QueryParameter("baseDirectory") String baseDirectory)
      throws IOException, ServletException
  {
    checkWritePermission();

    DynamicParameterConfiguration.INSTANCE.setBaseDirectory(baseDirectory);
    return redirectToIndex();
  }

  /**
   * Upload a script file to the class path directory.
   * @param request HTTP request
   * @return redirect to {@code index}
   * @throws Exception
   */
  public HttpResponse doUploadFile(StaplerRequest request) throws Exception
  {
    checkWritePermission();

    String classPathDirectory = request.getSubmittedForm().getString("classPathDirectory");
    lastDirectory = classPathDirectory;

    classPathDirectory = StringUtils.defaultIfEmpty(classPathDirectory, DEFAULT_NAME);
    File directory = getRebasedFile(classPathDirectory);

    if (!directory.exists())
    {
      directory.mkdirs();
    }

    FileItem fileItem = request.getFileItem("file");
    String fileName = Util.getFileName(fileItem.getName());
    if (!StringUtils.isEmpty(fileName))
    {
      fileItem.write(new File(directory, fileName));
    }

    return redirectToIndex();
  }

  /**
   * Delete the specified file.
   * @param filePath file path
   * @return redirect to {@code index}
   */
  public HttpResponse doDeleteFile(@QueryParameter("file") String filePath) throws IOException
  {
    checkWritePermission();

    File file = getRebasedFile(filePath);
    FileUtils.deleteQuietly(file);

    return redirectToIndex();
  }

  /**
   * Rename a file or a directory.
   * @param oldName old name
   * @param newName new name
   * @return redirect to {@code index}
   */
  public HttpResponse doRenameFile(@QueryParameter("oldName") String oldName,
      @QueryParameter("newName") String newName) throws IOException
  {
    checkWritePermission();

    File oldFile = getRebasedFile(oldName);
    File newFile = getRebasedFile(newName);
    oldFile.renameTo(newFile);

    return redirectToIndex();
  }

  // private methods

  private static void checkReadPermission()
  {
    Hudson.getInstance().checkPermission(Permission.READ);
  }

  private static void checkWritePermission()
  {
    Hudson.getInstance().checkPermission(Permission.CONFIGURE);
  }

  private static HttpResponse redirectToIndex()
  {
    return new HttpRedirect("index");
  }

  private static String getBaseDirectoryPath() throws IOException
  {
    return DynamicParameterConfiguration.INSTANCE.getBaseDirectory();
  }

  private static File getRebasedFile(String path) throws IOException
  {
    String basePath = getBaseDirectoryPath();
    File file = new File(path);
    if (!file.isAbsolute())
    {
      file = new File(basePath, path);
    }

    String canonicalFilePath = file.getCanonicalPath();
    if (FileUtils.isDescendant(basePath, canonicalFilePath))
    {
      return file;
    }
    else
    {
      // don't leave the base directory
      String fileName = new File(canonicalFilePath).getName();
      if(fileName.length() == 0)
      {
        // don't return the base directory in any case
        fileName = DEFAULT_NAME;
      }
      File rebasedFile = new File(basePath, fileName);
      return rebasedFile;
    }
  }

}
