package source;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Class which contains the basic structure of a relation
 */
class RelDef implements Serializable {
  private String relName;
  private int nb_col;
  private ArrayList<String> types_col = new ArrayList<>();
  private int fileIdx;
  private int recordSize;
  private int slotCount;

  RelDef(String relName, int nb_col, ArrayList<String> types_col, int fileIdx, int recordSize, int slotCount){
    this.relName = relName;
    this.nb_col = nb_col;
    this.types_col = types_col;
    this.fileIdx = fileIdx;
    this.recordSize = recordSize;
    this.slotCount = slotCount;
  }

  ArrayList<String> getTypes_col() {
    return types_col;
  }

  String getRelName() {
    return relName;
  }

  int getNb_col() {
    return nb_col;
  }

  int getFileIdx() {
    return fileIdx;
  }

  int getRecordSize() {
    return recordSize;
  }

  int getSlotCount() {
    return slotCount;
  }
}
