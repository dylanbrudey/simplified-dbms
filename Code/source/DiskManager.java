package source;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * Class that manage every disk access operation
 */
class DiskManager {
    private static DiskManager instance = null;

    private DiskManager() { }

    static DiskManager getInstance()
    {
        if(instance == null)
            instance = new DiskManager();
        return instance;
    }

    /**
     * create a new file related to an single relation
     * @param fileIdx file index
     */
    void createFile(int fileIdx) {
        try
        {
            String pathName = getAbsolutePathFromRelativePath(fileIdx);
            RandomAccessFile file = new RandomAccessFile(pathName,"rw");
            file.close();
        }catch(FileNotFoundException fn)
        {
            fn.printStackTrace();
            System.out.println("fichier non trouve/cree");
        }
        catch (IOException io)
        {
            io.printStackTrace();
        }

    }

    /**
     * add a new page into a file
     * @param   fileIdx file index
     * @return  PageId  pageId of the page
     */
    PageId addPage(int fileIdx)
    {
        PageId pageId;
        int pageNumber = 0;
        try
        {
            String pathName = getAbsolutePathFromRelativePath(fileIdx);
            RandomAccessFile file = new RandomAccessFile(pathName,"rw");
            int fileSize = (int) file.length();
            //create pageNumber by dividing the filesize by the size of a page, starts at page 0
            pageNumber = fileSize/Constants.getPageSize();
            //add the page in the file by increasing the file size
            file.setLength(fileSize + Constants.getPageSize());
            file.close();
        }catch(FileNotFoundException fn)
        {
            System.out.println("Erreur : ");
            fn.getMessage();
            System.out.println("fichier non trouve");
        }catch(IOException io)
        {
            System.out.println("Erreur : ");
            io.getMessage();
        }
        //creation of its pageId
        pageId = new PageId(fileIdx,pageNumber);
        return pageId;
    }

    /**
     * Read the selected page and fill a buffer with its data
     * @param   pageId  PageID instance
     * @param   buffer  Bytebuffer
     * @return  ByteBuffer buffer filled with data
     */
    ByteBuffer readPage(PageId pageId, ByteBuffer buffer)
    {
        buffer.rewind();
        int pageOffset;
        try
        {
            String pathName = getAbsolutePathFromRelativePath(pageId);
            RandomAccessFile file = new RandomAccessFile(pathName,"r");
            pageOffset =  pageId.getPageIdx() * Constants.getPageSize();
            file.seek(pageOffset);
            for(int i = 0; i < Constants.getPageSize() && buffer.hasRemaining(); i++)
            {
                buffer.put(file.readByte());
            }
            file.close();
        }catch(FileNotFoundException fn)
        {
            System.out.println("fichier non trouve");
            fn.getMessage();
        }catch(IOException io)
        {
            io.getMessage();
        }
        return buffer;
    }
    /**
     * Write on the selected page the contents in the buffer
     * @param   pageId  PageID instance
     * @param   buffer  Bytebuffer
     */
    void writePage(PageId pageId, ByteBuffer buffer)
    {
        buffer.rewind();
        int pageOffset;
        try
        {
            String pathName = getAbsolutePathFromRelativePath(pageId);
            RandomAccessFile file = new RandomAccessFile(pathName,"rw");
            pageOffset =  pageId.getPageIdx() * Constants.getPageSize();
            file.seek(pageOffset);
            for(int i = 0; i < Constants.getPageSize() && buffer.hasRemaining(); i++)
            {
                file.write(buffer.get());
            }
            file.close();
        }catch(FileNotFoundException fn)
        {
            System.out.println("fichier non trouve");
            fn.getMessage();
        }catch(IOException io)
        {
            io.getMessage();
        }
    }

    /**
     * Delete every file of the database in the DB directory
     */
    void cleanDatabase()
    {
        String pathName = getAbsolutePathFromRelativePath();
        File dir = new File(pathName);
        try
        {
            if(!dir.isDirectory()) {
                System.out.println("Rien à effacer.");
                return;
            }
            File[] listFiles = dir.listFiles();
            if(listFiles.length > 0)
            {
                for(File file : listFiles){
                    if(file.delete())
                        System.out.println(file.getName() + " supprime");
                    else
                        System.out.println(file.getName() + " non supprime");
                }
            }
        }catch(NullPointerException np) {
            np.printStackTrace();
            System.out.println("La commande \"clean\" n'a pas fonctionnee");
        }
    }

    private String getAbsolutePathFromRelativePath(int fileIdx)
    {
        File tempFile = new File("");
        return tempFile.getAbsoluteFile().getParentFile().getAbsolutePath() + "/DB/Data_" + fileIdx + ".rf";
    }

    private String getAbsolutePathFromRelativePath(PageId pageId)
    {
        File tempFile = new File("");
        return tempFile.getAbsoluteFile().getParentFile().getAbsolutePath() + "/DB/Data_" + pageId.getFileIdx() + ".rf";
    }

    private String getAbsolutePathFromRelativePath()
    {
        File tempFile = new File("");
        return tempFile.getAbsoluteFile().getParentFile().getAbsolutePath() + "/DB";
    }
}
