
package avl;

import VASSAL.build.module.BasicCommandEncoder;
import VASSAL.counters.CounterGlobalKeyCommand;
import VASSAL.counters.Decorator;
import VASSAL.counters.GamePiece;

public class AvlCommandEncoder extends BasicCommandEncoder {

  public Decorator createDecorator(String type, GamePiece inner) {
    if (type.startsWith(AvlSendToLocation.ID)) {
      return new AvlSendToLocation(type, inner);
    }
    else if (type.startsWith(CounterGlobalKeyCommand.ID)) {
      return new AvlCounterGlobalKeyCommand(type, inner);
    }
    return super.createDecorator(type, inner);
  }
}
