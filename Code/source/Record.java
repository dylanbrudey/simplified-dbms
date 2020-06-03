package source;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Contains the structure of a record associated to a relation along with its values list
 */
class Record {

    private RelDef reldef;
    private Rid rid;
    private ArrayList<String> values = new ArrayList<>();

    Record(RelDef reldef, ArrayList<String> values)
    {
        this.reldef = reldef;
        this.values = values;
    }

    Record(RelDef reldef, Rid rid)
    {
        this.reldef = reldef;
        this.rid = rid;
    }

    /**
     * write values of a record into the buffer, one by one
     * @param buffer    Bytebuffer buffer
     * @param position  position in the buffer
     */
    void writeToBuffer(ByteBuffer buffer, int position)
    {
        try
        {
            buffer.rewind();
            buffer.position(position);
            for(int j = 0; buffer.hasRemaining() && j < reldef.getTypes_col().size(); j++)
            {
                //int : stored with 4 bytes
                if(reldef.getTypes_col().get(j).equals("int"))
                    buffer.putInt(Integer.parseInt(values.get(j)));
                    //float : stored with 4 bytes
                else if(reldef.getTypes_col().get(j).equals("float"))
                    buffer.putFloat(Float.parseFloat(values.get(j)));

                    //string : stored with x*2 bytes  when x is the number of character of the string type (stringx, see reldef definition)
                else if(reldef.getTypes_col().get(j).startsWith("string"))
                {
                    //String current_string = reldef.getTypes_col().get(j).substring(6);
                    String currentString = values.get(j);
                    for(int h = 0; h < currentString.length() && buffer.hasRemaining(); h++)
                    {
                        buffer.putChar(currentString.charAt(h));
                    }
                }
            }
        }catch (NullPointerException np)
        {
            np.printStackTrace();
        }
    }

    /**
     * read from buffer values from a record, one by one
     * @param buffer    Bytebuffer buffer
     * @param position  position in the buffer
     */
    void readFromBuffer(ByteBuffer buffer, int position)
    {
        buffer.rewind();
        String columnType;
        buffer.position(position);
        for(int j = 0; buffer.hasRemaining() && j < reldef.getTypes_col().size(); j++)
        {
            columnType =  reldef.getTypes_col().get(j);
            switch(columnType)
            {
                case "int" :
                    values.add(String.valueOf(buffer.getInt()));
                break;

                case "float" :
                    values.add(String.valueOf(buffer.getFloat()));
                break;
                //String case
                default:
                    StringBuilder string_array = new StringBuilder();
                    int characNumber = Integer.parseInt(columnType.substring(6));
                    for(int h = 0; h < characNumber && buffer.hasRemaining(); h++)
                    {
                        string_array.append(buffer.getChar());
                    }
                        values.add(string_array.toString());
                break;
            }
        }
    }

    ArrayList<String> getValues() {
        return values;
    }

    void setRid(Rid rid) {
        this.rid = rid;
    }

    Rid getRid() {
        return rid;
    }

}
