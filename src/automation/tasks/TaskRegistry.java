package automation.tasks;

import java.util.HashMap;
import java.util.Map;

public class TaskRegistry {
    private static final Map<String, AutomationTask> tasks = new HashMap<>();
    
    static {
        // Register all tasks here
        register(new AddLeadTask());
        register(new IronmongeryImportTask());
        register(new GlassPartImportTask());
        register(new UploadGlassImagesTask());
        register(new UploadIronmongeryImagesTask());
        register(new UpdateIronmongeryDefaultsTask());
        register(new PageLoadTimeTask());
        register(new TimberCuttingListTask());
        register(new PriceFileRuleRemover());
        register(new PriceFileRuleAdder());
        register(new PropertyVisibilityImportTask());
        register(new LookupEditorImportTask());
        register(new DefaultIronmongeryImportTask());
        register(new DefaultValuesExportTask());
        register(new DefaultValuesImportTask());
     // In TaskRegistry
    }
    
    public static void register(AutomationTask task) {
        tasks.put(task.getName(), task);
    }
    
    public static AutomationTask getTask(String name) {
        return tasks.get(name);
    }
    
    public static String[] getTaskNames() {
        return tasks.keySet().toArray(new String[0]);
    }
}