package vddlogger;

import java.util.HashMap;

public class VddLogIssues {

   private HashMap<String, Integer> errors = null;
   private HashMap<String, Integer> wanrings = null;
   private HashMap<String, Integer> exceptions = null;

   public VddLogIssues () {

      this.errors = new HashMap<String, Integer>();
      this.wanrings = new HashMap<String, Integer>();
      this.exceptions = new HashMap<String, Integer>();

   }

   public HashMap<String, HashMap<String, Integer>> getData() {
      HashMap<String, HashMap<String, Integer>> data = new HashMap<String, HashMap<String,Integer>>();

      data.put("errors", this.errors);
      data.put("warnings", this.wanrings);
      data.put("exceptions", this.exceptions);

      return data;
   }

   public void addException(String str) {
      if (this.exceptions.containsKey(str)) {
         Integer tmp = this.exceptions.get(str);
         tmp += 1;
         this.exceptions.put(str, tmp);
      } else {
         this.exceptions.put(str, 1);
      }
   }

   public void addWarning(String str) {
      if (this.wanrings.containsKey(str)) {
         Integer tmp = this.wanrings.get(str);
         tmp += 1;
         this.wanrings.put(str, tmp);
      } else {
         this.wanrings.put(str, 1);
      }
   }

   public void addError(String str) {
      if (this.errors.containsKey(str)) {
         Integer tmp = this.errors.get(str);
         tmp += 1;
         this.errors.put(str, tmp);
      } else {
         this.errors.put(str, 1);
      }
   }

   public void appendIssues(VddLogIssues issues) {
      addIssue(issues.errors, this.errors);
      addIssue(issues.wanrings, this.wanrings);
      addIssue(issues.exceptions, this.exceptions);
   }

   private void addIssue(HashMap<String, Integer> src, HashMap<String, Integer> dst) {
      String[] keys = src.keySet().toArray(new String[0]);

      for (int i = 0; i <= keys.length -1; i++) {
         if (!dst.containsKey(keys[i])) {
            dst.put(keys[i], src.get(keys[i]));
         } else {
            int srcCount = src.get(keys[i]);
            int dstCount = dst.get(keys[i]);
            dst.put(keys[i], (srcCount + dstCount));
         }
      }
   }
}
