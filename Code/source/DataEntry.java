package source;

import java.util.ArrayList;

/**
 * Data entry structure
 */
public class DataEntry extends Entry {

    private ArrayList<Rid> rids;

    public DataEntry(int key)
    {
        super(key);
        rids = new ArrayList<>();
    }

    public void addRid(Rid rid)
    {
        rids.add(rid);
    }

    public ArrayList<Rid> getRids() {
        return rids;
    }
}
