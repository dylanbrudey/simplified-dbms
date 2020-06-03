package source;
import java.io.*;
import java.util.*;
/**
 * Command manager class, the entry  point of our DBMS
*/
class DBManager{

    private static DBManager instance = null;
    //Use of a private constructor to prevent the declaration of several instances
    private DBManager() {
    }

    /**
     * Get an instance of DBManager, hinder the use of the new operator and helps getting a singleton
     * @return an instance of DBManager
     */
    static DBManager getInstance() {
        if(instance == null)
            instance = new DBManager();
        return instance;
    }

    /**
     * Load the different elements needed while running the application if a configuration has been saved before
     */
    void init() {
        DBDef.getInstance().init();
        FileManager.getInstance().init();
    }

    /**
     * Save the different elements elements needed while running the application
     */
    private void finish() {
        DBDef.getInstance().finish();
        BufferManager.getInstance().flushAll();
    }

    /**
     * Process the command according to the arguments received by the user and execute it if the command exists in our DBMS
     * @param command       command received by the user
     */
    void processCommand(String command)
    {
        StringTokenizer receivedCommand = new StringTokenizer(command);
        StringBuilder action = new StringBuilder();
        StringBuilder relName = new StringBuilder();
        int columnsCount;
        String fileName;
        ArrayList<String> arguments = new ArrayList<>(30);
        int argumentsCount = 0;
        boolean executed = true;
        //split the command by arguments
        while(receivedCommand.hasMoreTokens()){
            switch (argumentsCount) {
                case 0:
                    action.append(receivedCommand.nextToken());
                    argumentsCount++;
                    break;
                case 1:
                    relName.append(receivedCommand.nextToken());
                    argumentsCount++;
                    break;
                default:
                    arguments.add(receivedCommand.nextToken());
                    argumentsCount++;
            }
        }
        //Choice here depends on the command first argument :"action"
        switch (action.toString()) {
            case "exit": finish();
                break;
            case "display": DBDef.getInstance().display();
                break;
            case "create":
                //arguments count without the action, the relation name and the number of columns
                int incompleteArgumentsCount = argumentsCount - 3;
                columnsCount = Integer.parseInt(arguments.get(0));
                arguments.remove(0);
                //check if the relation name already exists
                if (findMatchingReldef(relName.toString()) == null) {
                    //check if the right number of arguments has been given
                    if (incompleteArgumentsCount == columnsCount)
                        createRelation(relName.toString(), columnsCount, arguments);
                    else {
                        System.out.println("Le nombre de colonnes donne en 3eme argument ne correspond pas au nombre de colonnes indique par la suite");
                        executed = false;
                    }
                } else {
                    System.out.println("Une relation avec ce nom existe deja");
                    executed = false;
                }
                break;
            case "clean":
                    clean();
                break;
            case "insert":
                insertRelation(relName.toString(), arguments);
                break;
            case "insertall":
                fileName = "\\" + arguments.get(0);
                insertAll(fileName, relName.toString());
                break;
            case "selectall":
                selectAll(relName.toString());
                break;
            case "select": {
                //column 1 equals column index 0 in an array
                int colIdx = Integer.parseInt(arguments.get(0)) - 1;
                String value = arguments.get(1);
                select(relName.toString(), colIdx, value);
                break;
            }
            case "delete": {
                //column 1 equals column index 0 in an array
                int colIdx = Integer.parseInt(arguments.get(0)) - 1;
                String value = arguments.get(1);
                delete(relName.toString(), colIdx, value);
                break;
            }
            case "createindex": {
                //column 1 equals column index 0 in an array
                int colIdx = Integer.parseInt(arguments.get(0)) - 1;
                int order = Integer.parseInt(arguments.get(1));
                createIndex(relName.toString(), colIdx, order);
                break;
            }
            case "selectindex": {
                //column 1 equals column index 0 in an array
                int colIdx = Integer.parseInt(arguments.get(0)) - 1;
                int key = Integer.parseInt(arguments.get(1));
                selectIndex(relName.toString(), colIdx, key);
                break;
            }
            case "join": {
                //column 1 equals column index 0 in an array
                String relName2 = arguments.get(0);
                int colIdx = Integer.parseInt(arguments.get(1)) - 1;
                int colIdx2 = Integer.parseInt(arguments.get(2)) - 1;
                join(relName.toString(), relName2, colIdx, colIdx2);
                break;
            }
            default:
                System.out.println("Operation non reconnue :" + action.toString());
                executed = false;
                break;
        }
        if(!executed)
            System.out.println("Opération non effectuee\n");
    }

    /**
     * Create a new relation in our DBMS and add it to the list in the DBDef
     * @param relName       relation name
     * @param columnsCount  number of columns
     * @param columnsType   list of the type of each column
     */
    private void createRelation(String relName, int columnsCount, ArrayList<String> columnsType)
        {
            //Calculate the size of a record in this relation along with its slotCount
            int recordSize = 0;
            for(String col : columnsType)
            {
                if(col.equals("int") || col.equals("float"))
                    recordSize += 4;
                else if(col.startsWith("string"))
                    recordSize += Integer.parseInt(col.substring(6))*2;
            }
            int slotCount = Constants.getPageSize()/(recordSize+1);
            RelDef reldef = new RelDef(relName, columnsCount, columnsType, DBDef.getRelCount(), recordSize, slotCount);
            DBDef.getInstance().addRelation(reldef);
            FileManager.getInstance().createRelationFile(reldef);
        }

    /**
     * Reset the database and delete every file inside the DB directory
     */
    private void clean()
    {
        //Reset our instances of the bufferManager, the DBdef and the fileManager classes
        BufferManager.getInstance().reset();
        DBDef.getInstance().reset();
        FileManager.getInstance().reset();
        //Delete files in DB directory
        DiskManager.getInstance().cleanDatabase();
    }

    /**
     * Insert a record in the right relation
     * @param relName       relation name
     * @param columns       list of the information to insert for each column of the relation
     */
    private void insertRelation(String relName, ArrayList<String> columns)
    {
        for(RelDef reldef : DBDef.getInstance().getRelDefs())
        {
            if(reldef.getRelName().equals(relName))
            {
                Record record = new Record(reldef,columns);
                record.setRid(FileManager.getInstance().insertRecordInRelation(record,relName));
            }
        }
    }

    /**
     * Insert records from a file in the matching relation
     * @param fileName      file name
     * @param relName       relation name
     */
    private void insertAll(String fileName, String relName)
    {
        //find a existing match for the relation name among reldefs in the dbdef and retrieve the matching reldef
        RelDef reldef = findMatchingReldef(relName);
        //Read the file and insert the records line by line
        try
        {
            //create a reading stream in the file
            File tempFile = new File("");
            //look for the csv file at our project root
            String pathName = tempFile.getAbsoluteFile().getParentFile().getAbsolutePath() + fileName;
            FileReader fileReader = new FileReader(pathName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            StringTokenizer sortLine;
            ArrayList<String> values = new ArrayList<>();
            //Read the file line by line
            while((line = bufferedReader.readLine()) != null)
            {
                sortLine = new StringTokenizer(line);
                //split the line by comma and create a list of values of a single record
                while(sortLine.hasMoreTokens())
                {
                    values.add(sortLine.nextToken(","));
                }
                //create a record with the selected reldef (earlier,see beginning of the method and the list of values
                Record record = new Record(reldef,values);
                //insert the record at the right place and assign to it its rid
                record.setRid(FileManager.getInstance().insertRecordInRelation(record,relName));
                values.clear();
            }
        }catch(FileNotFoundException fn)
        {
            fn.printStackTrace();
            System.out.println("fichier non trouvé !");
        }catch(IOException io)
        {
            io.printStackTrace();
        }
    }

    /**
     * Select every record from a relation
     * @param relName       relation name
     */
    private void selectAll(String relName)
    {
        ArrayList<Record> records = FileManager.getInstance().selectAllFromRelation(relName);
        displayRecords(records,false);
    }

    /**
     * Select every record from a relation with a precise value on the selected column
     * @param relName       relation name
     * @param colIdx        index of the selected column
     * @param value         selected value
     */
    private void select(String relName, int colIdx, String value)
    {
        ArrayList<Record> records = FileManager.getInstance().selectFromRelation(relName,colIdx,value);
        displayRecords(records,false);
    }

    /**
     * Delete every record from a relation with a precise value on the selected column
     * @param relName       relation name
     * @param colIdx        index of the selected column
     * @param value         selected value
     */
    private void delete(String relName,  int colIdx, String value)
    {
        //select the eligible records
        ArrayList<Record> records = FileManager.getInstance().selectFromRelation(relName,colIdx,value);
        //delete records one by one
        for(Record record : records)
        {
            FileManager.getInstance().deleteRecordFromRelation(record,relName);
        }
        displayRecords(records,true);
    }

    /**
     * Create an index for a relation on a specific column
     * @param relName       relation name
     * @param colIdx        index of the selected column
     * @param order         order of the B+tree
     */
    private void createIndex(String relName, int colIdx, int order)
    {
        Tree tree = new Tree(null,order);
        Index index = new Index(relName,colIdx,tree);
        tree.setRoot(new TreeNode(null));
        DBDef.getInstance().addIndex(index);
        ArrayList<Leaf> leaves = FileManager.getInstance().createIndexFromRelation(index,relName,colIdx,order);
        index.getTree().bulkLoad(leaves);
    }

    /**
     *  select records from a relation using the index on the selected column
     * @param relName       relation name
     * @param colIdx        index of the selected column
     * @param key           searched key
     */
    private void selectIndex(String relName, int colIdx, int key)
    {
        ArrayList<Rid> rids = new ArrayList<>();
        ArrayList<Record> records = new ArrayList<>();
        for(Index index : DBDef.getInstance().getIndexes())
        {
            //look for the right index in the database
            if(index.getRelName().equals(relName) && index.getColIdx() == colIdx)
            {
                //search for the record ids corresponding to the given key
                rids = index.getTree().getRoot().searchChild(key);
            }
        }
        if(rids != null)
        {
            System.out.println("Rids size" + rids.size());
            //sort rids by pageIdx to make less disk access (a lot less shifting between pages)
            Collections.sort(rids);
            records = FileManager.getInstance().selectIndexFromRelation(rids,relName);
        }
        displayRecords(records,false);
    }

    /**
     * Calculate the equi-join of 2 relations on 2 columns using their index and display the resulting tuples
     * @param relName       relation name
     * @param relName2      second relation name
     * @param colIdx        index of the selected column in the 1st relation
     * @param colIdx2       index of the selected column in the 2nd relation
     */
    private void join(String relName, String relName2, int colIdx, int colIdx2)
    {
        ArrayList<Record> tuples = FileManager.getInstance().joinRelations(relName, relName2, colIdx, colIdx2);
        displayRecords(tuples,false);
    }

    /**
     * find a existing match for the relation name among reldefs in the dbdef and retrieve the matching reldef
     * @param   relName relation name
     * @return  RelDef  corresponding reldef
     */
    private RelDef findMatchingReldef(String relName)
    {
        RelDef reldef = null;
        for(RelDef reldefFound : DBDef.getInstance().getRelDefs())
        {
            if(reldefFound.getRelName().equals(relName))
                reldef = reldefFound;
        }
        return reldef;
    }

    /**
     * display records, deleted or not, on the console
     * @param records   list of records
     * @param isDeleted true if the records are deleted, false if they aren't
     */
    private void displayRecords(ArrayList<Record> records, boolean isDeleted)
    {
        System.out.println();
        for(Record record : records)
        {
            ArrayList<String> values = record.getValues();
            for(int i = 0; i < values.size(); i++) {
                if(i < values.size()-1)
                    System.out.print(values.get(i) + " ; ");
                else
                    System.out.print(values.get(i));
            }
            System.out.println();
        }
        System.out.println();
        if(isDeleted)
            System.out.println("Total deleted records = " + records.size());
        else
            System.out.println("Total records = " + records.size());
        System.out.println();
    }
}
