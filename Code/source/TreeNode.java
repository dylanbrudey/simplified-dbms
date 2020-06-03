package source;

import java.util.ArrayList;

/**
 * TreeNode structure
 */
public class TreeNode extends Node {
    //an index entry without a key, represents the first pointer in every node that isn't a leaf
    private Node firstChildNode = null;

    TreeNode(TreeNode parent)
    {
        super(parent);
    }

    /**
     * Look for the selected key in the node, the search will vary depending on the type of node
     * TreeNode type : Look for the key node, if found, continue the search with the child node of the corresponding
     * index entry, if not, compare one last time the key we are looking for and the chosen key, then select the child
     * node of the entry depending on the result
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
            if(key > entryKey)
                min = choice + 1;
            else if(key < entryKey)
                max = choice - 1;
            else
                found = true;
        }
            if(!found) {
                choice = (min + max) / 2;
                entryKey = entries.get(choice).getKey();
                if (key < entryKey) {
                    if (choice == 0)
                    {
                        if(firstChildNode instanceof TreeNode)
                            return ((TreeNode) firstChildNode).searchChild(key);
                        else
                            return ((Leaf) firstChildNode).searchChild(key);
                    }
                    else
                        choice--;
                }
            }
            IndexEntry entry = (IndexEntry) entries.get(choice);
            return entry.getChildNode().searchChild(key);
    }

    void addEntry(IndexEntry indexEntry)
    {
        entries.add(indexEntry);
    }

    Node getFirstChildNode() {
        return firstChildNode;
    }

    void setFirstChildNode(Node firstChildNode) {
        this.firstChildNode = firstChildNode;
    }
}
