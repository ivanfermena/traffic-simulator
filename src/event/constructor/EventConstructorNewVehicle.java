package event.constructor;

import event.Event;
import event.EventNewVehicle;
import ini.IniSection;

public class EventConstructorNewVehicle extends EventConstructor {

    public EventConstructorNewVehicle() {
        this.tag = "new_vehicle";
        this.keys = new String[] { "time", "id" , "max_speed", "itinerary"};
        this.defaultValues = new String[] { "", "", "",""};
    }

    @Override
    public Event parser(IniSection section) {

        if (!section.getTag().equals(this.tag) ||
                section.getValue("type") != null) return null;
        else
            return new EventNewVehicle(
                    // extrae el valor del campo “time” en la sección
                    // 0 es el valor por defecto en caso de no especificar el tiempo
                    EventConstructor.parseIntNoNegative(section, "time", 0),
                    // extrae el valor del campo “id” de la sección
                    EventConstructor.validID(section, "id"),
                    EventConstructor.parseIntNoNegative(section, "max_speed", 0),
                    section.getValue("itinerary").split(",") );
    }

    @Override
    public String toString() { return "New Vehicle"; }
}
