package source;

/**
 * Class that contains the page Identification structure needed to find a page in a file
 */
public class PageId {
    private int fileIdx;
    private int pageIdx;

    PageId(int fileIdx, int pageIdx)
    {
        this.fileIdx = fileIdx;
        this.pageIdx = pageIdx;
    }

    int getFileIdx() {
        return fileIdx;
    }

    int getPageIdx() {
        return pageIdx;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof PageId))return false;
        boolean isEqual = false;
        PageId pageId2 = (PageId) obj;
        if(this.fileIdx == pageId2.fileIdx && this.pageIdx == pageId2.pageIdx)
        isEqual = true;
        return isEqual;
    }
}
