
package avd;


import VASSAL.build.module.BasicCommandEncoder;
import VASSAL.counters.Decorator;
import VASSAL.counters.GamePiece;

public class AvdCommandEncoder extends BasicCommandEncoder {

  public Decorator createDecorator(String type, GamePiece inner) {
    if (type.startsWith(AvdRestricted.ID)) {
      return new AvdRestricted(type, inner);
    }
    return super.createDecorator(type, inner);
  }
}
