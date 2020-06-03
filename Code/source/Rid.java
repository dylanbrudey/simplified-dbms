package source;

/**
 * Record identifier which allows to locate a record within a page, the latter itself within a file
 */
public class Rid implements Comparable<Rid> {
    private PageId pageId;
    private int slotIdx;

    Rid(PageId pageId, int slotIdx)
    {
        this.pageId = pageId;
        this.slotIdx = slotIdx;
    }

    PageId getPageId() {
        return pageId;
    }

    int getSlotIdx() {
        return slotIdx;
    }

    @Override
    public int compareTo(Rid o) {
        return this.pageId.getPageIdx() - ((Rid) o).pageId.getPageIdx();
    }
}
