# KotlinScripter

To run the project, you can either run the prebuilt `KotlinScripter.jar` file, located in `KotlinScripter`, or you can take the following steps to build it yourself:

Clone the project and open it in IntelliJ – then select **Build | Build Artifacts**. Now point to the created **.jar** and select 
**Build**. You should now find the project at `KotlinScripter/out/artifact/KotlinScripter_jar/KotlinScripter.jar`.
                                              
Running this file, we find the editor pane on the left and the output pane on the right. Any script written in the editor
pane will be written to a new file and run after pressing the `Run Script` button in the bottom left. Scripts that
 have been written but are not yet ready to be run can be saved with the `Save` button in the top right and opened
  later with the `Open` button. The `New` button wipes the script that's currently in the editor so that you can
   start on a new script.
   
During the running process, the live output is displayed to the pane on the right, as well as any errors that might
 arise from execution or the interpretation of the script. When the script is running, the label at the top left
  corner of the output pane will change to _"Running..."_ to indicate that the script is running, and it will then
   change to _"Done!"_ to indicate that the script has finished. After the script has finished running, the exit code
    from the script is displayed next to the top right corner of the output pane, letting us know if the exit code
     was non-zero or not.
     
Lastly, during the scripting process, keywords, like `as`, `break`, `class`, etc. are highlighted with an orange
 color to indicate that they are keywords. These keywords are stored in a `String[]` in the class
  `KeywordDocumentFilter`, located within the `GUI.java` file. To add more keywords, simply add more entries to the
   array, and they'll be parsed out. Furthermore, all comments are greyed out, and keywords that appear in comments
    won't be highlighted.
    
    