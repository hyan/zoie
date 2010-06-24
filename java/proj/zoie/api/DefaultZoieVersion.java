package proj.zoie.api;

public class DefaultZoieVersion extends ZoieVersion
{
  private String _versionDesp; 
  private long _versionId;
 
//hao: format is "_versionDesp + VID:_versionId";
  public String encodeToString()
  {
    return _versionDesp + ";ZOIEVID:" + _versionId;
  }
  public void setVersionDesp(String str)
  {
    _versionDesp = str;
  }
  public void setVersionId(long id)
  {
    _versionId = id;
  }
  public long getVersionId()
  {
    return _versionId;
  }
  public int compareTo(ZoieVersion o)
  {
    if (this == o) return 0;
    if(o == null) return 1;
    DefaultZoieVersion oo = (DefaultZoieVersion)o;
    if(_versionId < oo._versionId)
      return -1;
    else if(_versionId > oo._versionId)
      return 1;
    else
    {
      return this.encodeToString().compareToIgnoreCase(oo.encodeToString());
    }    
  } 
  public String toString()
  {
    return this.encodeToString();
  }
}