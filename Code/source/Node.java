package source;

import java.util.ArrayList;

/**
 * Class that contains the node structure and how to browse the B+tree
 */
public abstract class Node {
    //Every parent isn't a leaf, so no need to make parent a Node object
    private TreeNode parent;
    ArrayList<Entry> entries;

    Node(TreeNode parent)
    {
        this.parent = parent;
        this.entries = new ArrayList<>();
    }

    /**
     * Look for the selected key in the node, the search will vary depending on the type of node
     * TreeNode type : Look for the key node, if found, continue the search with the child node of the corresponding
     * index entry, if not, compare one last time the key we are looking for and the chosen key and select the child
     * node of the entry depending on the result
     * Leaf type : Look for the key node, if found, return the corresponding data entry's rids list to select the
     * records we want, if not, return null
     * @param key value on the column we're looking for
     * @return  rids list
     */
    public abstract ArrayList<Rid> searchChild(int key);

    TreeNode getParent() {
        return parent;
    }

    public ArrayList<Entry> getEntries() {
        return entries;
    }

    void setEntries(ArrayList<Entry> entries) {
        this.entries = entries;
    }

    void setParent(TreeNode parent) {
        this.parent = parent;
    }
}
