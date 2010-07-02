package proj.zoie.api;

public class DefaultZoieVersion extends ZoieVersion
{
  private long _versionId;
 
  public String toString()
  {    
    return String.valueOf(_versionId);
  }
  
  public String encodeToString()
  {
    return toString();
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
      return 0;
    }    
  }   
}