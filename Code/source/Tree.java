package source;

import java.util.ArrayList;

/**
 * Class that contains the B+tree structure and how to make one from scratch
 */
class Tree {
    private TreeNode root;
    private int order;
    private ArrayList<Node> nodes = new ArrayList<>();

    Tree(TreeNode root, int order)
    {
        this.root = root;
        this.order = order;
    }

    /**
     * Create a B+tree using a leaves list sorted by the keys of their entries
     * @param leaves leaves list
     */
    void bulkLoad(ArrayList<Leaf> leaves) {
        boolean firstTime = true;
        addNode(root);
        TreeNode currentNode = (TreeNode) nodes.get(0);
        //we insert the tree leaf by leaf and split the nodes created when necessary
        for (Leaf leaf : leaves) {
            //add the current leaf to the nodes list
            addNode(leaf);
            Leaf currentLeaf = (Leaf) nodes.get(nodes.size() - 1);
            int indexEntrySize = currentNode.getEntries().size();
            //node empty, first pointer towards a leaf
            if (currentNode.getFirstChildNode() == null) {
                //if the leaf we work on is the first one to insert in the node, we don't need to put a key
                currentNode.setFirstChildNode(currentLeaf);
                currentLeaf.setParent(currentNode);
            }
            //node not empty
            else {
                //node not full, we can add more pointers towards leaves on this node
                if (indexEntrySize < 2 * order) {
                    int key = currentLeaf.getEntries().get(0).getKey();
                    currentNode.addEntry(new IndexEntry(key, currentLeaf));
                    currentLeaf.setParent(currentNode);
                }
                //node completely filled, split required
                else if (indexEntrySize == 2 * order) {
                    //add an entry with the current leaf that will be part of the new node after the split
                    int key = currentLeaf.getEntries().get(0).getKey();
                    currentNode.addEntry(new IndexEntry(key, currentLeaf));
                    TreeNode nextNode = null;
                    //we split the current node then check its parent to see if a split is also required until it isn't
                    do {
                        //the current node is root
                        if (currentNode.getParent() == null) {
                            addNode(new TreeNode(null));
                            setRoot((TreeNode) nodes.get(getNodes().size() - 1));
                            currentNode.setParent((TreeNode) nodes.get(getNodes().size() - 1));
                            //create a pointer from the new root to the old one, a new intermediary node (called tree node here)
                            currentNode.getParent().setFirstChildNode(currentNode);
                        }
                        addNode(new TreeNode(currentNode.getParent()));
                        TreeNode newNode = (TreeNode) nodes.get(getNodes().size() - 1);
                        //we update the next node we're going to visit once the split on the whole tree is done
                        if (firstTime) {
                            nextNode = newNode;
                            firstTime = false;
                            currentLeaf.setParent(newNode);
                        }
                        ArrayList<Entry> currentEntries = currentNode.getEntries();
                        //split the node in two, with one entry (in the middle) that goes to the parent
                        IndexEntry indexEntry = (IndexEntry) currentEntries.get(currentEntries.size() / 2);
                        ArrayList<Entry> entries1 = new ArrayList<>(currentEntries.subList(0, (currentEntries.size() / 2)));
                        ArrayList<Entry> entries2 = new ArrayList<>(currentEntries.subList(((currentEntries.size() / 2) + 1), currentEntries.size()));
                        currentNode.setEntries(entries1);
                        //set parent of the entries (on the right of the middle) to the newly-made node
                        for (Entry entry : entries2) {
                            IndexEntry indexEntry2 = (IndexEntry) entry;
                            indexEntry2.getChildNode().setParent(newNode);
                        }
                        newNode.setEntries(entries2);
                        newNode.setFirstChildNode(indexEntry.getChildNode());
                        indexEntry.getChildNode().setParent(newNode);
                        indexEntry.setChildNode(newNode);
                        currentNode.getParent().getEntries().add(indexEntry);
                        //change the current node to the parent and check if a split is needed
                        currentNode = currentNode.getParent();
                    } while (currentNode.getEntries().size() > 2 * order);
                    currentNode = nextNode;
                    firstTime = true;
                }
            }
        }
        }

    private void addNode(Node node)
    {
        nodes.add(node);
    }

    private ArrayList<Node> getNodes() {
        return nodes;
    }

    void setRoot(TreeNode root) {
        this.root = root;
    }

    TreeNode getRoot() {
        return root;
    }

}
