package proj.zoie.api;


public abstract class ZoieVersion implements Comparable<ZoieVersion>
{
  public abstract String encodeToString();
  //public abstract boolean hasNext();
  //public abstract ZoieVersion next();
  /*public int compareTo(ZoieVersion o) 
  {
       return this.hashCode() - o.hashCode();
  }*/
}