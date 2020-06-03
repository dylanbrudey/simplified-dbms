package source;

import java.util.Scanner;

public class Main{
  public static void main(String[] args)
  {
      DBManager.getInstance().init();
      String command = "";
      Scanner scanner = new Scanner(System.in);
      System.out.println("Il est imperatif de quitter (avec \"exit/quitter/q\") pour sauvegarder les relations et les donnees encore en memoire vive\n");
      boolean redirection = Boolean.parseBoolean(args[0]);
      //Command process
      do{
          if(!redirection)
              System.out.println("Entrez une commande :");
          //when the file has been entirely read, exit
          if(!scanner.hasNextLine() && redirection)
              command = "exit";
          else {
              command = scanner.nextLine();
          }
          DBManager.getInstance().processCommand(command);
        }while(!command.equals("exit"));
      scanner.close();
  }
}
