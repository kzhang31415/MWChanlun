package study;

import com.motivewave.platform.sdk.common.Defaults;
import com.motivewave.platform.sdk.common.NVP;
import com.motivewave.platform.sdk.common.desc.ColorDescriptor;
import com.motivewave.platform.sdk.common.desc.DiscreteDescriptor;
import com.motivewave.platform.sdk.common.desc.StringDescriptor;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.StudyHeader;

import java.util.ArrayList;
import java.util.List;

@StudyHeader(
        namespace = "com.unfinished",
        id = "study_1",
        name = "Tutorial",
        desc = "Tutorial",
        menu = "Youtube",
        menu2 = "Custom Studies",
        overlay = true,
        helpLink = "https://github.com/kldcty/ChanlunX"
)

public class Tutorial extends Study{
    @Override
    public void initialize(Defaults defaults){
        var sd = createSD();
        var tab = sd.addTab("General Settings");

        // Dropdown List
        List<NVP> dropdown = new ArrayList();
        dropdown.add(new NVP("Letter A", "A"));
        dropdown.add(new NVP("Letter B", "B"));
        dropdown.add(new NVP("Letter C", "C"));

        var general = tab.addGroup("General");
        general.addRow(new ColorDescriptor("Color", "Color", defaults.getRed()));
        general.addRow(new StringDescriptor("String", "String", ""));
        general.addRow(new DiscreteDescriptor("Letter", "Letter", "A", dropdown));

        sd.addQuickSettings("Color");
        sd.addQuickSettings("String");

    }

    public void onLoad(Defaults defaults){
        var color = getSettings().getColor("Color");
        var string = getSettings().getString("String");
        var letter = getSettings().getString("Letter");


        // debug | info | warning | error
        info("Color: " + color + "  String: " + string);
        info("Letter: " + letter);
    }
}