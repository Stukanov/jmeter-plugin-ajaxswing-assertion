package com.epam.jmeter.plugins.iii.ajaxswing.assertion.gui;

import com.epam.jmeter.plugins.iii.ajaxswing.assertion.AjaxSwingAssertion;
import java.awt.BorderLayout;
import javax.swing.*;
import kg.apc.jmeter.JMeterPluginsUtils;
import org.apache.jmeter.assertions.gui.AbstractAssertionGui;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.gui.JLabeledTextField;

/**
 * Created by Evgenii_Stukanov on 10.01.2018.
 */

public class AjaxSwingAssertionGui extends AbstractAssertionGui  {
    private static final long serialVersionUID = 1L;
    private JLabeledTextField searchFor = null;
    private JComboBox<String> searchType = null;
    private JCheckBox invert = null;
    private static final String WIKIPAGE = "AjaxSwingAssertion";

    public AjaxSwingAssertionGui() {
        this.init();
    }

    public void init() {
        this.setLayout(new BorderLayout());
        this.setBorder(this.makeBorder());
        this.add(JMeterPluginsUtils.addHelpLinkToPanel(this.makeTitlePanel(), "AjaxSwingAssertion"), "North");
        VerticalPanel panel = new VerticalPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        this.searchFor = new JLabeledTextField("Search for: ");
        this.searchType = new JComboBox(new String[]{"substring", "xpath", "regex"});
        this.invert = new JCheckBox("Invert assertion (will fail if search condition was met)");
        panel.add(new JLabel("Search type:"));
        panel.add(this.searchType);
        panel.add(this.searchFor);
        panel.add(this.invert);
        this.add(panel, "Center");
    }

    public void clearGui() {
        super.clearGui();
        this.searchFor.setText("div");
        this.invert.setSelected(false);
        //this.searchType.setSelectedIndex(0);
    }

    public TestElement createTestElement() {
        AjaxSwingAssertion asAssertion = new AjaxSwingAssertion();
        this.modifyTestElement(asAssertion);
        asAssertion.setComment(JMeterPluginsUtils.getWikiLinkText("AjaxSwingAssertion"));
        return asAssertion;
    }

    public String getLabelResource() {
        return this.getClass().getSimpleName();
    }

    public String getStaticLabel() {
        return "<epam> AjaxSwing Assertion";
    }

    public void modifyTestElement(TestElement element) {
        super.configureTestElement(element);
        if(element instanceof AjaxSwingAssertion) {
            AjaxSwingAssertion asAssertion = (AjaxSwingAssertion)element;
            asAssertion.setSearchFor(this.searchFor.getText());
            asAssertion.setInvert(this.invert.isSelected());
            asAssertion.setSearchType(this.searchType.getSelectedItem().toString());
        }

    }

    public void configure(TestElement element) {
        super.configure(element);
        AjaxSwingAssertion asAssertion = (AjaxSwingAssertion)element;
        this.searchFor.setText(asAssertion.getSearchFor());
        this.invert.setSelected(asAssertion.isInvert());
        this.searchType.setSelectedItem(asAssertion.getSearchType());
    }

}
