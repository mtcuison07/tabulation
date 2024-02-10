import java.math.BigDecimal;
import java.sql.SQLException;
import org.guanzon.tabulation.Callback;
import org.guanzon.tabulation.Tabulate;
import org.junit.AfterClass;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.agentfx.CommonUtils;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class testTabulate {
    static GRider instance = new GRider();
    static Tabulate trans;
    static Callback callback;
    
    @BeforeClass
    public static void setUpClass() {   
        String path;
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            path = "D:/GGC_Java_Systems";
        }
        else{
            path = "/srv/GGC_Java_Systems";
        }
        System.setProperty("sys.default.path.config", path);
        
        if (!instance.logUser("Tabulation", "M001111122")){
            System.err.println(instance.getMessage() + instance.getErrMsg());
            System.exit(1);
        }
        
        trans = new Tabulate(instance);
        
        callback = new Callback() {
            @Override
            public void MasterRetreive(int fnIndex, Object foValue) {
                System.out.println("MasterRetreived for index " + fnIndex + " with value " + foValue);
            }

            @Override
            public void DetailRetreive(int fnRow, int fnIndex, Object foValue) {
                
            }
        };
        
        trans.setCallback(callback);
    }
    
    @AfterClass
    public static void tearDownClass() {
        callback = null;
        trans = null;
        instance = null;
    }
    
    @Test
    public void test01NewTransaction() {
        try {
            //get the event id event selection form
            //get the judge name from judge entry form
                        
            //create new transaction
            if (!trans.NewTransaction()) fail(trans.getMessage());

            //pass the event id that you got from the event selection form
            trans.setMaster("sEventIDx", "0001");
            
            //accept judge name
            trans.setMaster("sJudgeNme", "Michael Cuison");
            
            //load event criteria
            if (!trans.LoadDetail()) fail(trans.getMessage());
            
            //load the event participants
            if (!trans.LoadParticipants(false, 3)){
                fail(trans.getMessage());
            }

            //display criteria to ui
            int lnRow = trans.getCriteriaCount();
            for (int lnCtr = 0; lnCtr <= lnRow - 1; lnCtr ++){
                System.out.println("Criteria " + (lnCtr + 1) + ": " + trans.getCriteria(lnCtr, "sCriteria").toString().toUpperCase());
            }
            // TOTAL
            System.out.println("Criteria  : TOTAL");
            
            
            //display to ui
            lnRow = trans.getParticipantsCount();
            for (int lnCtr = 0; lnCtr <= lnRow - 1; lnCtr ++){
                //participant info
                System.out.print("NO: " + trans.getParticipants(lnCtr, "sGroupNox"));
                System.out.print(" ID: " + trans.getParticipants(lnCtr, "sGroupIDx"));
                System.out.println(" NAME: " + trans.getParticipants(lnCtr, "sGroupNme")); 
                
                //participant scores
                //set the group id to the master first
                trans.setMaster("sGroupIDx", trans.getParticipants(lnCtr, "sGroupIDx"));
             
                //reload criteria and score
                if (!trans.LoadDetail()) fail(trans.getMessage());
                
                int crit = trans.getCriteriaCount();
                double lnTotal = 0.00;
                for (int x = 0; x <= crit - 1; x++){                    
                    System.out.println(" Crit " + (x + 1) + ":" + CommonUtils.NumberFormat((BigDecimal) trans.getCriteria(x, "nPercentx"), "#,#0.00"));
                    lnTotal += Double.valueOf(String.valueOf(trans.getCriteria(x, "nPercentx")));
                }        
                System.out.println(" Total :" + CommonUtils.NumberFormat(lnTotal, "#,#0.00"));
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
}
