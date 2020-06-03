package source;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Allows us to read and write records and therefore process commands
 */
class HeapFile {
    private RelDef reldef;

    HeapFile(RelDef relDef)
    {
        this.reldef = relDef;
    }

    /**
     * Create a new disk file associated to a relation with a set header page
     */
    void createNewOnDisk()
    {
        //Create the disk file
        DiskManager.getInstance().createFile(reldef.getFileIdx());
        //Add the header page
        PageId headerPageId = DiskManager.getInstance().addPage(reldef.getFileIdx());
        ByteBuffer buffer = BufferManager.getInstance().getPage(headerPageId);
        for(int i = 0; i < Constants.getPageSize() && buffer.hasRemaining();i++) {
            //initialize dataPageCount and data pages' slot counts
                buffer.putInt(0);
        }
        buffer.rewind();
        BufferManager.getInstance().freePage(new PageId(reldef.getFileIdx(),0),true);
    }

    /**
     * Add a data page into a file
     * @return  PageId of the new data page
     */
    private PageId addDataPage()
    {
        PageId pageId = DiskManager.getInstance().addPage(reldef.getFileIdx());
        //Set the bytes of the newly-added data page to 0
        ByteBuffer buffer = BufferManager.getInstance().getPage(pageId);
        for(int i = 0; i < Constants.getPageSize() && buffer.hasRemaining();i++) {
            buffer.put((byte)0);
        }
        buffer.rewind();
        BufferManager.getInstance().freePage(pageId,true);
        //Retrieve the Header Page and update it
        PageId headerPageId = new PageId(reldef.getFileIdx(),0);
        ByteBuffer bufferHeaderPage = BufferManager.getInstance().getPage(headerPageId);
        //Update the number of data pages in the file registered by the Header Page
        bufferHeaderPage.rewind();
        int dataPageCount = bufferHeaderPage.getInt();
        dataPageCount++;
        bufferHeaderPage.rewind();
        bufferHeaderPage.putInt(dataPageCount);
        //Update the slotCount of the newly-added data page in the Header Page
        bufferHeaderPage.putInt(Constants.getIntSize()*pageId.getPageIdx(),reldef.getSlotCount());
        bufferHeaderPage.rewind();
        BufferManager.getInstance().freePage(headerPageId,true);
        return pageId;
    }

    /**
     * Look for a free data page
     * @return  PageId if a free page is found, else null
     */
    private PageId getFreeDataPageId()
    {
        PageId pageId = null;
        PageId headerPageId = new PageId(reldef.getFileIdx(),0);
        ByteBuffer bufferHeaderPage = BufferManager.getInstance().getPage(headerPageId);
        bufferHeaderPage.rewind();
        //Use the data page count to make sure a data page exists, if it does, look for a free one, if it doesn't,
        // return null
        int dataPageCount = bufferHeaderPage.getInt();
        bufferHeaderPage.position(4);
        boolean found = false;
        int pageIdx = 0;
        for(int i = 1; !found && i <= dataPageCount && bufferHeaderPage.hasRemaining(); i++)
        {
            int availableSlot = bufferHeaderPage.getInt();
            if(availableSlot > 0)
            {
                found = true;
                pageIdx = i;
            }
        }
        BufferManager.getInstance().freePage(headerPageId,false);
        if(found)
            pageId = new PageId(reldef.getFileIdx(),pageIdx);
        return pageId;
    }

    /**
     * Write a record into the selected data page
     * @param record    Record
     * @param pageId    PageId
     * @return Rid from the record
     */
    private Rid writeRecordToDataPage(Record record, PageId pageId)
    {
        //the current data page is considered free
        boolean found = false;
        //soon-to-be-used slot position where the selected record will be written
        int slotIdx = 0;
        ByteBuffer bufferDataPage = BufferManager.getInstance().getPage(pageId);
        bufferDataPage.rewind();
        int byteMapPosition = 0;
        for(int i = 0; i < reldef.getSlotCount() && !found; i++)
        {
            byteMapPosition = i;
            //found is set at true when a byte at 0 (free) is found in the bytemap
            found = bufferDataPage.get() == 0;
           if(found)
               slotIdx = reldef.getSlotCount() + reldef.getRecordSize()*i;
        }
        record.writeToBuffer(bufferDataPage,slotIdx);
        //update the bytemap -> mark the used slot as occupied (byte at 1)
        bufferDataPage.rewind();
        bufferDataPage.put(byteMapPosition,(byte)1);
        BufferManager.getInstance().freePage(pageId,true);
        //Retrieve the Header Page and update it
        PageId headerPageId = new PageId(reldef.getFileIdx(),0);
        ByteBuffer bufferHeaderPage = BufferManager.getInstance().getPage(headerPageId);
        //Update the number of slots of the selected data page registered by the Header Page
        bufferHeaderPage.rewind();
        //Update the slotCount of the data page in the Header Page, here the slotCount represents the number of
        // available slots
        int slotCount = bufferHeaderPage.getInt(pageId.getPageIdx()*Constants.getIntSize());
        slotCount--;
        bufferHeaderPage.rewind();
        bufferHeaderPage.putInt(pageId.getPageIdx()*Constants.getIntSize(),slotCount);
        bufferHeaderPage.rewind();
        BufferManager.getInstance().freePage(headerPageId,true);
        return new Rid(pageId,slotIdx);
    }

    /**
     * Get a list of records from a data page by reading them one by one
     * @param pageId        PageId
     * @return list of records
     */
    private ArrayList<Record>  getRecordsInDataPage(PageId pageId)
    {
        ArrayList<Record> recordsList = new ArrayList<>();
        ByteBuffer bufferDataPage = BufferManager.getInstance().getPage(pageId);
        bufferDataPage.rewind();

        int recordsCount = 0, slotIdx;
        boolean found;
        for(int i = 0; bufferDataPage.hasRemaining() && i < reldef.getSlotCount(); i++)
        {
            bufferDataPage.rewind();
            bufferDataPage.position(i);
            //found is set at true when a byte at 1 (occupied) is found in the bytemap
            found = bufferDataPage.get() == 1;
            if(found)
            {
                //Start the buffer position after the bytemap and at the right record
                slotIdx = reldef.getSlotCount() + reldef.getRecordSize()*i;
                recordsList.add(new Record(reldef,new Rid(pageId,slotIdx)));
                recordsList.get(recordsCount).readFromBuffer(bufferDataPage,slotIdx);
                recordsCount++;
            }
        }
        BufferManager.getInstance().freePage(pageId,false);
        return recordsList;
    }

    /** Retrieve the records present in the data page
     * @param currentDataPageNumber int
     * @return list of records present in the data page
     */
    ArrayList<Record> getRecordsInDataPage(int currentDataPageNumber)
    {
        ArrayList<Record> records = null;
        //Retrieve the Header Page and pick the data page count
        PageId headerPageId = new PageId(reldef.getFileIdx(),0);
        ByteBuffer bufferHeaderPage = BufferManager.getInstance().getPage(headerPageId);
        bufferHeaderPage.rewind();
        int dataPageCount = bufferHeaderPage.getInt();
        if(currentDataPageNumber <= dataPageCount)
        {
            bufferHeaderPage.rewind();
            bufferHeaderPage.position(currentDataPageNumber*Constants.getIntSize());
            int availableSlots = bufferHeaderPage.getInt();
            //check the number of occupied slots for each data page and get their records
            if(availableSlots < reldef.getSlotCount())
                records = getRecordsInDataPage(new PageId(reldef.getFileIdx(),currentDataPageNumber));
            //records should only return null when the number of the data page we want to explore exceeds the total
            //number of data page
            else
                records = new ArrayList<>();
        }
        //free the page as soon as we don't use it anymore
        BufferManager.getInstance().freePage(headerPageId,false);
       return records;
    }

    /**
     * Insert a record
     * @param record  Record
     * @return record's rid
     */
    Rid insertRecord(Record record)
    {
        //Look for a free data page
        PageId pageId = getFreeDataPageId();
        if(pageId == null)
            pageId = addDataPage();
        //Use the free data page to insert the record and return its rid
        return writeRecordToDataPage(record, pageId);
    }

    /**
     * Select every record from the heapfile
     * @return  records list
     */
    ArrayList<Record> getAllRecords()
    {
        ArrayList<Record> recordList = new ArrayList<>();
        //Retrieve the Header Page and pick the data page count
        PageId headerPageId = new PageId(reldef.getFileIdx(),0);
        ByteBuffer bufferHeaderPage = BufferManager.getInstance().getPage(headerPageId);
        bufferHeaderPage.rewind();
        int dataPageCount = bufferHeaderPage.getInt();
        //index starts at 1 because Header Page is at 0, without checking if the buffer has remaining space because the buffer position is moved by another method, making it go out of this loop
        for(int i = 1; i <= dataPageCount; i++)
        {
            //The header is often used, put the position back where we were
            bufferHeaderPage.rewind();
            bufferHeaderPage.position(i*Constants.getIntSize());
            int availableSlots = bufferHeaderPage.getInt();
            //check the number of occupied slots for each data page and get their records
            if(availableSlots < reldef.getSlotCount())
                recordList.addAll(getRecordsInDataPage(new PageId(reldef.getFileIdx(),i)));

        }
        BufferManager.getInstance().freePage(headerPageId,false);
        return recordList;
    }

    RelDef getReldef() {
        return reldef;
    }

    /**
     * delete a record from the heapfile
     * Change the slot indicator from 1 to 0 to show the record "doesn't exist" anymore and that it's a free usable slot,
     * no need to replace its data since the slot won't be visited until it's replaced by another record
     * @param pageId        PageId
     * @param slotIdx       position of the record in the page
     */
    void deleteRecordFromDataPage(PageId pageId, int slotIdx)
    {
        int byteMapPosition;
        ByteBuffer bufferDataPage = BufferManager.getInstance().getPage(pageId);
        bufferDataPage.rewind();
        //Update the bytemap -> mark the used slot as free (byte at 0)
        byteMapPosition = (slotIdx-reldef.getSlotCount())/reldef.getRecordSize();
        bufferDataPage.rewind();
        bufferDataPage.put(byteMapPosition,(byte)0);
        BufferManager.getInstance().freePage(pageId,true);
        //Retrieve the Header Page and update it
        PageId headerPageId = new PageId(reldef.getFileIdx(),0);
        ByteBuffer bufferHeaderPage = BufferManager.getInstance().getPage(headerPageId);
        bufferHeaderPage.rewind();
        //Update the number of slots (slotCount) of the selected data page registered by the Header Page, here the slotCount represents the number of available slots
        int slotCount = bufferHeaderPage.getInt(pageId.getPageIdx()*Constants.getIntSize());
        slotCount++;
        bufferHeaderPage.rewind();
        bufferHeaderPage.putInt(pageId.getPageIdx()*Constants.getIntSize(),slotCount);
        BufferManager.getInstance().freePage(headerPageId,true);
    }

    /**
     * select every record from the heapfile using their rids
     * @param rids          rids list, each associated to record from the same relation
     * @return  records list
     */
    ArrayList<Record> getAllRecordsFromRids(ArrayList<Rid> rids)
    {
        ArrayList<Record> records = new ArrayList<>();
        PageId currentPageId = rids.get(0).getPageId();
        ByteBuffer bufferDataPage = BufferManager.getInstance().getPage(currentPageId);
        int recordsCount = 0;
        for(Rid rid : rids)
        {
            if(!rid.getPageId().equals(currentPageId))
            {
                BufferManager.getInstance().freePage(currentPageId,false);
                bufferDataPage = BufferManager.getInstance().getPage(rid.getPageId());
                currentPageId = rid.getPageId();
            }
            records.add(new Record(reldef,new Rid(rid.getPageId(),rid.getSlotIdx())));
            records.get(recordsCount).readFromBuffer(bufferDataPage,rid.getSlotIdx());
            recordsCount++;
        }
        BufferManager.getInstance().freePage(currentPageId,false);
        return records;
    }
}
