package source;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Class that manages the files, each associated to a relation in the database
 */
class FileManager {
    private ArrayList<HeapFile> heapFiles = new ArrayList<>();
    private static FileManager instance = null;

    private FileManager()
    {
    }

    static FileManager getInstance()
    {
        if(instance == null)
            instance = new FileManager();
        return instance;
    }

    /**
     * Reset the fileManager singleton
     */
    void reset()
    {
        heapFiles.clear();
    }

    /**
     * Initialize the file manager
     */
    void init()
    {
        //associate each relation with a heapfile
        for (RelDef relDef : DBDef.getInstance().getRelDefs()) {
            heapFiles.add(new HeapFile(relDef));
        }
    }

    /**
     * create a file associated with a relation
     * @param relDef        Reldef instance
     */
    void createRelationFile(RelDef relDef)
    {
        HeapFile heapFile = new HeapFile(relDef);
        heapFiles.add(heapFile);
        heapFile.createNewOnDisk();
    }

    /**
     * insert a record in a relation
     *
     * @param record        Record
     * @param relName       relation name
     * @return record's rid
     */
    Rid insertRecordInRelation(Record record, String relName)
    {
        Rid rid = null;
        for (HeapFile heapFile : heapFiles) {
            String relNameFromHeapFile = heapFile.getReldef().getRelName();
            if (relNameFromHeapFile.equals(relName))
                rid = heapFile.insertRecord(record);
        }
        return rid;
    }

    /**
     * Select every record from the selected relation
     *
     * @param relName       relation name
     * @return records list
     */
    ArrayList<Record> selectAllFromRelation(String relName)
    {
        ArrayList<Record> recordList = new ArrayList<>();
        for (HeapFile heapFile : heapFiles) {
            String relNameFromHeapFile = heapFile.getReldef().getRelName();
            if (relNameFromHeapFile.equals(relName))
                recordList = heapFile.getAllRecords();
        }
        return recordList;
    }

    /**
     * Select every record from a relation with a precise value on the selected column
     * @param relName       relation name
     * @param colIdx        column index
     * @param value         selected value
     * @return records list
     */
    ArrayList<Record> selectFromRelation(String relName, int colIdx, String value)
    {
        //recordList : record list with the right relation name, will be sorted with the right conditions secondly
        ArrayList<Record> recordList = new ArrayList<>();
        for (HeapFile heapFile : heapFiles) {
            RelDef relDef = heapFile.getReldef();
            if (relDef.getRelName().equals(relName))
                recordList = heapFile.getAllRecords();
        }
        Iterator iterator = recordList.iterator();
        while (iterator.hasNext()) {
            Record record = (Record) iterator.next();
            if (!record.getValues().get(colIdx).equals(value))
                iterator.remove();
        }
        return recordList;
    }

    /**
     * Delete the record from a relation with a precise value on the selected column
     * @param record        soon-to-be deleted record
     * @param relName       relation name
     */
    void deleteRecordFromRelation(Record record, String relName)
    {
        for (HeapFile heapFile : heapFiles) {
            String relNameFromHeapFile = heapFile.getReldef().getRelName();
            if (relNameFromHeapFile.equals(relName))
                heapFile.deleteRecordFromDataPage(record.getRid().getPageId(), record.getRid().getSlotIdx());
        }
    }

    /**
     * Create index from a relation
     * @param index         Index instance
     * @param relName       relation name
     * @param colIdx        index column
     * @param order         order of the B+tree
     * @return leaves list
     */
    ArrayList<Leaf> createIndexFromRelation(Index index, String relName, int colIdx, int order)
    {
        //recordList : record list with the right relation name, will be sorted with the right conditions secondly
        ArrayList<Record> recordList = new ArrayList<>();
        for (HeapFile heapFile : heapFiles) {
            RelDef relDef = heapFile.getReldef();
            if (relDef.getRelName().equals(relName))
                recordList = heapFile.getAllRecords();
        }
        boolean keepOnGoing;
        //sort the record by the column selected by the user
        do {
            keepOnGoing = false;
            for (int i = 0; i < recordList.size() - 1; i++) {
                int value1 = Integer.parseInt(recordList.get(i).getValues().get(colIdx));
                int value2 = Integer.parseInt(recordList.get(i + 1).getValues().get(colIdx));
                if (value1 > value2) {
                    Record record = recordList.get(i + 1);
                    recordList.set(i + 1, recordList.get(i));
                    recordList.set(i, record);
                    keepOnGoing = true;
                }
            }
        }
        while (keepOnGoing);
        //Assign records to keys to make DataEntries and order them by leaf
        ArrayList<Leaf> leaves = new ArrayList<>();
        int lastKey = Integer.parseInt(recordList.get(0).getValues().get(colIdx));
        leaves.add(new Leaf(null));
        int leavesCount = 0;
        int dataEntriesCount = 0;
        DataEntry dataEntry;
        leaves.get(leavesCount).addEntry(new DataEntry(lastKey));
        for (Record record : recordList) {
            int key = Integer.parseInt(record.getValues().get(colIdx));
            //"key" of the current record different than the last one
            if (key != lastKey) {
                //leaf is full, create a new one along with a new data entry
                if (leaves.get(leavesCount).getEntries().size() == 2 * order) {
                    leaves.add(new Leaf(null));
                    leavesCount++;
                    dataEntriesCount = 0;
                    leaves.get(leavesCount).addEntry(new DataEntry(key));
                }
                //stay on the same leaf
                else {
                    leaves.get(leavesCount).addEntry(new DataEntry(key));
                    dataEntriesCount++;
                }
                lastKey = key;
            }
            //add the record in the current dataEntry
            dataEntry = (DataEntry) leaves.get(leavesCount).getEntries().get(dataEntriesCount);
            dataEntry.addRid(record.getRid());
        }
        return leaves;
    }

    /**
     * Select an index from a relation
     *
     * @param rids          rids list
     * @param relName       relation name
     * @return records list
     */
    ArrayList<Record> selectIndexFromRelation(ArrayList<Rid> rids, String relName)
    {
        ArrayList<Record> records = new ArrayList<>();
        for (HeapFile heapFile : heapFiles) {
            String relNameFromHeapFile = heapFile.getReldef().getRelName();
            if (relNameFromHeapFile.equals(relName))
                records = heapFile.getAllRecordsFromRids(rids);
        }
        return records;
    }

    /**
     * Calculate the equi-join of 2 relations on 2 columns using their index
     * @param relName       relation name
     * @param relName2      second relation name
     * @param colIdx        index of the selected column in the 1st relation
     * @param colIdx2       index of the selected column in the 2nd relation
     * @return list of the resulting tuples
     */
    ArrayList<Record> joinRelations(String relName, String relName2, int colIdx, int colIdx2)
    {
        ArrayList<Record> tuples = new ArrayList<>();
        ArrayList<Record> recordsFromPageRel1;
        int currentDataPageNumber = 1;
        //Look for the corresponding heapfile for each relation
        HeapFile heapFile1 = findMatchingHeapFile(relName);
        HeapFile heapFile2 = findMatchingHeapFile(relName2);
        //Loop until there aren't any more data pages to explore in the first relation
        while((recordsFromPageRel1 = heapFile1.getRecordsInDataPage(currentDataPageNumber)) != null)
        {
            currentDataPageNumber++;
            //Get all tuples of the second relation, page by page
            ArrayList<Record> recordsFromRel2 = heapFile2.getAllRecords();
            //Compare the tuples from the 1st relation's one page
            for(Record R1 : recordsFromPageRel1) {
                for(Record R2 : recordsFromRel2) {
                    if(R1.getValues().get(colIdx).equals(R2.getValues().get(colIdx2)))
                    {
                        ArrayList<String> values = new ArrayList<>(R1.getValues());
                        values.addAll(R2.getValues());
                        tuples.add(new Record(heapFile1.getReldef(),values));
                    }
                }
            }
        }
        return tuples;
    }

    /**
     * find a existing match for the relation name among heapfiles in the file manager and retrieve it
     * @param relName relation name
     * @return Heapfile with the same relation name
     */
    private HeapFile findMatchingHeapFile(String relName)
    {
        HeapFile selectedHeapfile = null;
        for (HeapFile heapFile : heapFiles) {
            String relNameFromHeapFile = heapFile.getReldef().getRelName();
            if (relNameFromHeapFile.equals(relName))
                selectedHeapfile = heapFile;
        }
        return selectedHeapfile;
    }
}