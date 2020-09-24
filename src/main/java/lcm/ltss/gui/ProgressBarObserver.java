package lcm.ltss.gui;

import java.awt.FlowLayout;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;


public class ProgressBarObserver extends JPanel implements Observer
{
    private static final long serialVersionUID = 1L;

    JProgressBar prg;
    ObservableValue pv;
    JLabel jl = null;
    
    public ProgressBarObserver(ObservableValue ov)
    {
        this.pv = ov;
        
        prg = new JProgressBar(0,100);
        prg.setValue(ov.getIntValue());
        prg.setStringPainted(true);
        jl = new JLabel(ov.getStringValue());
        this.add(jl);
        this.add(prg);

        //JButton btnClose = new JButton("Close");
        //btnClose.addActionListener(e -> System.exit(0));
        //jp.add(btnClose);
      
    }
    
    public void update(Observable o, Object arg)
    {
        if (pv == o)
        {
            prg.setValue(pv.getIntValue());
            jl.setText(pv.getStringValue());
        }
        
    }
    
    public static void main(String[] args)
    {
        final ObservableValue ov1 = new ObservableValue(0, "abc");
        final ObservableValue ov2 = new ObservableValue(100, "cba");
     
        SwingUtilities.invokeLater(new Runnable() {

            public void run()
            {
               
                ProgressBarObserver jp1 = new ProgressBarObserver(ov1);
                ProgressBarObserver jp2 = new ProgressBarObserver(ov2);
                JFrame frame = new JFrame();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setLayout(new FlowLayout());
                frame.setSize(300,300);
                frame.add(jp1);
                frame.add(jp2);
                //frame.add(jp);
               // frame.add(btnClose);
                frame.setVisible(true);
                ov1.addObserver(jp1);
                ov2.addObserver(jp2);
                
            }});
        
        for (int ii = 1; ii <= 100; ++ii)
        {
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            ov1.setValues(ii, "" + ii);
            ov2.setValues(100-ii, "" + (100-ii) );
        }
        
    }
    
}