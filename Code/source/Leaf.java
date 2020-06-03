package source;

import java.util.ArrayList;

/**
 * Leaf structure
 */
public class Leaf extends Node {

    Leaf(TreeNode parent)
    {
        super(parent);
    }

    /**
     * Look for the selected key in the node, the search will vary depending on the type of node
     * Leaf type : Look for the key node, if found, return the corresponding data entry's rids list to select the
     * records we want, if not, return null
     * @param key value on the column we're looking for
     * @return  rids list
     */
    @Override
    public ArrayList<Rid> searchChild(int key)
    {
        boolean found = false;
        int min = 0, max = entries.size()-1;
        int choice = 0;
        int entryKey;
        while(!found && min <= max)
        {
            choice = (min+max)/2;
            entryKey = entries.get(choice).getKey();
			      if(key > entryKey) {
				min = choice + 1;
			}
            else if(key < entryKey)
			{
				max = choice - 1;
			}
            else if(key == entryKey)
			{
				found = true;
			}
        }
        if(found)
        {
            DataEntry entry = (DataEntry) entries.get(choice);
            return entry.getRids();
        }
        //case where the key hasn't been found in the leaf
        else
            return null;

    }

    void addEntry(DataEntry dataEntry)
    {
        this.entries.add(dataEntry);
    }

    @Override
    public ArrayList<Entry> getEntries() {
        return super.getEntries();
    }
}
