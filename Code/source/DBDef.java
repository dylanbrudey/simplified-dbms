package source;

import java.io.*;
import java.util.*;

/**
 * Class that contains the database schema information
 */
class DBDef{

  private static int relCount = 0;
  private ArrayList<RelDef> relDefs = new ArrayList<RelDef>();
  private ArrayList<Index> indexes = new ArrayList<>();
  private static DBDef instance = new DBDef();

  private DBDef() {
  }

  /**
   * Get an instance of DBDef, hinder the use of the new operator and helps getting a singleton
   * @return an instance of DBDef
   */
  static DBDef getInstance() {
    return instance;
  }

  /**
   * Reset the attributes of the instance
   */
  void reset()
  {
    relCount = 0;
    relDefs.clear();
  }
  /**
   * Load and set the attributes from saved files
   */
  void init()
  {
    ObjectInputStream stream = null;
    try
    {
      File tempFile = new File("");
      String pathName = tempFile.getAbsoluteFile().getParentFile().getAbsolutePath() + "/DB/Catalog.def";
      FileInputStream file = new FileInputStream(pathName);
      stream = new ObjectInputStream(file);
      relCount = stream.readInt();
      for(int i = 0; i < relCount; i++)
      {
        relDefs.add((RelDef)stream.readObject());
      }
    } catch (IOException io) {
      if(io instanceof FileNotFoundException)
      {
        System.out.println();
        System.out.println("Le fichier Catalog.def n existe pas, aucune sauvegarde de DBDef n'est donc a charger");
        System.out.println();
      }

    } catch(ClassNotFoundException cnf)
    {
      cnf.printStackTrace();
    }
    finally {
      try
      {
        if (stream != null)
        {
          stream.close();
        }
      } catch (final IOException ex)
      {
        ex.printStackTrace();
      }
    }

  }
  /**
   * Save the attributes to files
   */
  void finish()
  {
    ObjectOutputStream stream = null;
    try
    {
      File tempFile = new File("");
      String pathName = tempFile.getAbsoluteFile().getParentFile().getAbsolutePath() + "/DB/Catalog.def";
      FileOutputStream file = new FileOutputStream(pathName);
      stream = new ObjectOutputStream(file);
      stream.writeInt(relCount);
      for(RelDef relDef : relDefs)
      {
        stream.writeObject(relDef);
      }
    }catch(IOException io)
    {
      io.printStackTrace();
    }
    finally {
      try
      {
        if (stream != null)
        {
          stream.flush();
          stream.close();
        }
      } catch (final IOException ex)
      {
        ex.printStackTrace();
      }
    }
  }

  /**
   * add a relation to the database
   * @param reldef  reldef instance
   */
  void addRelation(RelDef reldef){
    relDefs.add(reldef);
    relCount++;
  }

  /**
   * add an index to the database
   * @param index index instance
   */
  void addIndex(Index index)
  {
    indexes.add(index);
  }
  //Method to delete
  void display()
  {
    System.out.println("\nVoici la liste des relations : ");
    for(RelDef r : relDefs)
    {
      System.out.print(r.getRelName() + " " + r.getNb_col() + " ");
      for(String chaine : r.getTypes_col())
      {
        System.out.print(chaine + " ");
      }
      System.out.println();
    }
    System.out.println("Nombre de relations = " + relCount + "\n");
  }

  static int getRelCount() {
    return relCount;
  }

  ArrayList<RelDef> getRelDefs() {
    return relDefs;
  }

  ArrayList<Index> getIndexes() {
    return indexes;
  }
}
