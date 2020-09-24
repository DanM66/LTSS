package lcm.ltss.gui;

import java.util.Observable;
public class ObservableValue extends Observable
{
   private int n = 0;
   private String str = null;
   public ObservableValue(int n)
   {
      setIntValue(n);
   }
   
   public ObservableValue(int n, String str)
   {
      setValues(n,str);
   }
   
   public void setIntValue(int n)
   {
      this.n = n;
      setChanged();
      notifyObservers();
   }
   public void setValues(int n, String str)
   {
       this.n = n;
       this.str = str;
       setChanged();
       notifyObservers();
   }
   
   public int getIntValue()
   {
      return n;
   }
   
   public String getStringValue()
   {
      return str;
   }
}