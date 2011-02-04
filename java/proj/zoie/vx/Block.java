package proj.zoie.vx;

import java.util.List;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;

import proj.zoie.api.ZoieIndexReader;
import proj.zoie.api.indexing.ZoieIndexable.IndexingReq;

public class Block
{
  private boolean _readonly = false;
  final Directory _dir;
  public Block(Directory dir)
  {
    _dir = dir;
  }
  List<ZoieIndexReader<?>> getReaders()
  {
    return null;
  }
  void putData(List<IndexingReq> indexingreq)
  {
    if (_readonly) throw new IllegalStateException(this + " has been sealed and is readonly now.");
  }
//  void combine(Block block)
//  {
//    IndexWriter writer = null;
//    try
//    {
//      writer = openIndexWriter(null,null);
//      writer.addIndexesNoOptimize(new Directory[] { block._dir });
//    }
//    finally
//    {       
//      closeIndexWriter();
//    }
//  }
  
  /**
   * After this method is called, the block is read only.
   */
  void seal()
  {
    _readonly = true;
  }
}
