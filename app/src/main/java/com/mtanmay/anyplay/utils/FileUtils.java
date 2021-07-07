package com.mtanmay.anyplay.utils;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import static com.mtanmay.anyplay.Constants.DIR_AUDIO;
import static com.mtanmay.anyplay.Constants.DIR_CACHE;
import static com.mtanmay.anyplay.Constants.DIR_IMAGES;

/**
 * Utility class for file operations
 */
public class FileUtils {

    /** Saves file in the specified directory type
     * @param context The context to use
     * @param dirType type of directory. Must be one of {@code DIR_AUDIO}, {@code DIR_IMAGE}, {@code DIR_CACHE}
     * @param bytes bytes to be saved
     * @param fileName name of the file to be created
     * @return true if file is saved successfully, false otherwise
     */
    public static boolean saveFile(Context context, String dirType, byte[] bytes, String fileName) {

        File file;
        try {
            file = getFile(context, dirType, fileName);
            BufferedOutputStream buffer = new BufferedOutputStream(new FileOutputStream(file));
            buffer.write(bytes);
            buffer.flush();
            buffer.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return file.exists();
    }


    /**
     * Returns the bytes of file
     * @param file to be read
     * @return bytes of the file if file read successfully else returns {@code null}
     * @throws FileNotFoundException if file is not found
     */
    public static byte[] readFile(File file) throws FileNotFoundException {

        if(file == null)
            return null;

        byte[] contents = new byte[(int)file.length()];
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));

        try {
            bis.read(contents);
            bis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return contents;
    }

    public static File getFile(Context context, String dirType, String fileName) {

        if(dirType.equals(DIR_IMAGES)) {
            File dir = new File(context.getExternalFilesDir(null) + File.separator + DIR_IMAGES + File.separator);
            if(!dir.exists())
                dir.mkdir();

            return new File(dir, fileName);
        }
        else if(dirType.equals(DIR_AUDIO)) {
            File dir = new File(context.getExternalFilesDir(null) + File.separator + DIR_AUDIO + File.separator);
            if(!dir.exists())
                dir.mkdir();

            return new File(dir, fileName);
        }
        else if(dirType.equals(DIR_CACHE)) {
            File dir = new File(context.getCacheDir().getAbsolutePath());
            return new File(dir, fileName);
        }

        return null;

    }

    /**
     * creates a temporary file created in the cache directory
     * @param context context of the application
     * @param fileType type of file to create i.e. audio (.m4a) or image (.png)
     * @param bytes bytes to write to the file
     * @param fileName name of the file to be created
     * @return File created in the application cache
     * @throws Exception
     */
    public static File getTempFile(Context context, String fileType, byte[] bytes, String fileName) throws Exception{

        File file;
        if(fileType.equals("audio"))
            file = getFile(context, DIR_CACHE, fileName+".m4a");
        else if(fileType.equals("img"))
            file = getFile(context, DIR_CACHE, fileName+".png");
        else
            return null;

        file.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(bytes);
        fos.close();
        return file;
    }

}
