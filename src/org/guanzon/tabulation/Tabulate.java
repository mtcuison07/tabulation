package org.guanzon.tabulation;

import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.constants.EditMode;

public class Tabulate {
    private GRider _app;
    private CachedRowSet _master;
    private CachedRowSet _detail;
    private CachedRowSet _participants;
    private Callback _callback;
    
    private int _editmode;
    private String _message;
    
    public Tabulate(GRider foValue){
        _app = foValue;
        
        _master = null;
        _detail = null;
        _participants = null;
        
        _editmode = EditMode.UNKNOWN;
    }
    
    public void setCallback(Callback foValue){
        _callback = foValue;
    }
    
    public String getMessage(){
        return _message;
    }
    
    public int getMasterCount() throws SQLException{
        _master.last();
        return _master.getRow();
    }
    
    public int getCriteriaCount() throws SQLException{
        _detail.last();
        return _detail.getRow();
    }
    
    public int getParticipantsCount() throws SQLException{
        _participants.last();
        return _participants.getRow();
    }
    
    public void setMaster(String fsIndex, Object foValue) throws SQLException{
        setMaster(getColumnIndex(_master, fsIndex), foValue);
    }
    
    public void setMaster(int fnIndex, Object foValue) throws SQLException{
        _master.first();
        _master.updateObject(fnIndex, foValue);
        _master.updateRow();
        
        if (_callback != null) _callback.MasterRetreive(fnIndex, _master.getObject(fnIndex));
    }
    
    public Object getMaster(int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        
        _master.first();
        return _master.getObject(fnIndex);
    }
    
    public Object getMaster(String fsIndex) throws SQLException{
        return getMaster(getColumnIndex(_master, fsIndex));
    }
    
    public Object getParticipants(int fnRow, int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        
        _participants.absolute(fnRow + 1);
        return _participants.getObject(fnIndex);
    }
    
    public Object getParticipants(int fnRow, String fsIndex) throws SQLException{
        return getParticipants(fnRow, getColumnIndex(_participants, fsIndex));
    }
    
    public Object getCriteria(int fnRow, int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        
        _detail.absolute(fnRow + 1);
        return _detail.getObject(fnIndex);
    }
    
    public Object getCriteria(int fnRow, String fsIndex) throws SQLException{
        return getCriteria(fnRow, getColumnIndex(_detail, fsIndex));
    }
    
    public boolean NewTransaction(){
        if (_app == null) {
            _message = "Application driver is not set.";
            return false;
        }
        
        if (!(_editmode == EditMode.READY || _editmode == EditMode.UNKNOWN)){
            _message = "Transaction is on update mode. Save or cancel the transaction first.";
            return false;
        }
        
        ResultSet loRS = null;
        
        try {
            String lsSQL;
            RowSetFactory factory = RowSetProvider.newFactory();

            //open master
            lsSQL = MiscUtil.addCondition(getSQ_Master(), "0=1");
            loRS = _app.executeQuery(lsSQL);
            
            _master = factory.createCachedRowSet();
            _master.populate(loRS);
            MiscUtil.close(loRS);
            
            _master.last();
            _master.moveToInsertRow();
            MiscUtil.initRowSet(_master);
            _master.insertRow();
            _master.moveToCurrentRow();
        } catch (SQLException e) {
            _message = e.getMessage();
            MiscUtil.close(loRS);
            return false;
        }
        
        _editmode = EditMode.ADDNEW;
        return true;
        
    }
    
    public boolean LoadDetail(){
        ResultSet loRS = null;
        
        try {
            String lsSQL;
            RowSetFactory factory = RowSetProvider.newFactory();

            lsSQL = getSQ_Detail();
            
            lsSQL += " AND b.sGroupIDx = " + SQLUtil.toSQL((String) getMaster("sGroupIDx")) +
                        " AND b.sComptrID = " + SQLUtil.toSQL(_app.getComputerID()) +
                    " ORDER BY a.nEntryNox";
            
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sEventIDx = " + SQLUtil.toSQL((String) getMaster("sEventIDx")));
                        
            loRS = _app.executeQuery(lsSQL);
            
            _detail = factory.createCachedRowSet();
            _detail.populate(loRS);
            MiscUtil.close(loRS);
            
            _detail.last();
            if (_detail.getRow() <= 0){
                _message = "Criteria is not set for this event.";
                return false;
            }
            
            _detail.first();
        } catch (SQLException e) {
            _message = e.getMessage();
            MiscUtil.close(loRS);
            return false;
        }
        
        return true;
    }

    public boolean LoadParticipants(boolean fbArrange, int fnOrderTp){
        ResultSet loRS = null;
        
        try {
            String lsSQL;
            RowSetFactory factory = RowSetProvider.newFactory();

            lsSQL = MiscUtil.addCondition(getSQ_Participants(), "a.sEventIDx = " + SQLUtil.toSQL((String) getMaster("sEventIDx")));
            
            if (!fbArrange){
                lsSQL = lsSQL + " ORDER BY CONVERT(a.sGroupNox, UNSIGNED INTEGER)";
            } else {
                if (fnOrderTp == 2){
                    lsSQL = lsSQL + " ORDER BY b.nRatingsx DESC, CONVERT(a.sGroupNox, UNSIGNED INTEGER) ASC";
                } else {
                    lsSQL = lsSQL + " ORDER BY b.nRatingsx " + (fnOrderTp == 0 ? "ASC" : "DESC") + " , " + "a.sGroupNox ASC";
                }
            }

            loRS = _app.executeQuery(lsSQL);
            
            _participants = factory.createCachedRowSet();
            _participants.populate(loRS);
            MiscUtil.close(loRS);
            
            if (getParticipantsCount() <= 0){
                _message = "No participants for this event.";
                return false;
            }
        } catch (SQLException e) {
            _message = e.getMessage();
            MiscUtil.close(loRS);
            return false;
        }
        
        return true;
    }
    
    private String getSQ_Master(){
        return "SELECT" +
                    "  a.sGroupIDx" +
                    ", a.nJudgeNox" +
                    ", a.sJudgeNme" +
                    ", a.nRatingsx" +
                    ", b.sGroupNme" +
                    ", b.sEventIDx" +
                    ", b.sGroupNox" +
                    ", a.sComptrID" +
                " FROM Event_Tabulation a" +
                    ", Event_Participants b" +
                " WHERE a.sGroupIDx = b.sGroupIDx";
    }
    
    private String getSQ_Detail(){
        return "SELECT" +
                     "  b.sGroupIDx" +
                     ", b.nJudgeNox" +
                     ", b.nEntryNox" +
                     ", IFNULL(b.nPercentx, 0.00) nPercentx" +
                     ", a.sCriteria" +
                     ", a.nPercentx xPercentx" +
                     ", a.nEntryNox xEntryNox" +
                     ", a.sEventIDx" +
                     ", b.sComptrID"  +
                  " FROM Event_Criteria a" +
                     " LEFT JOIN Event_Tabulation_Detail b" +
                        " ON a.nEntryNox = b.nEntryNox";
    }
    
    private String getSQ_Participants(){
        return "SELECT" +
                    "  a.sGroupIDx" +
                    ", a.sGroupNme" +
                    ", a.sGroupNox" +
                    ", a.nMemberxx" +
                    ", IFNULL(b.nRatingsx, 0) nRatingsx" +
                    ", b.nJudgeNox" +
                " FROM Event_Participants a" +
                    " LEFT JOIN Event_Tabulation b" +
                        " ON a.sGroupIDx = b.sGroupIDx" +
                        " AND b.sComptrID = " + SQLUtil.toSQL(_app.getComputerID());
    }
    
    public String getSQ_Browse(){
        return "SELECT" +
                    "  a.sGroupIDx" +
                    ", b.sGroupNme" +
                    ", a.sJudgeNme" +
                    ", c.sEventNme" +
                " FROM Event_Tabulation a" +
                    ", Event_Participants b" +
                    ", Events c" +
                " WHERE a.sGroupIDx = b.sGroupIDx" +
                    " AND b.sEventIDx = c.sEventIDx";
    }
    
    private int getColumnIndex(CachedRowSet loRS, String fsValue) throws SQLException{
        int lnIndex = 0;
        int lnRow = loRS.getMetaData().getColumnCount();
        
        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            if (fsValue.equals(loRS.getMetaData().getColumnLabel(lnCtr))){
                lnIndex = lnCtr;
                break;
            }
        }
        
        return lnIndex;
    }
}