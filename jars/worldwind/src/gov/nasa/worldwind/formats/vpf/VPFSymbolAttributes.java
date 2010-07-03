package gov.nasa.worldwind.formats.vpf;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.Logging;

import java.awt.*;

/**
 * @author Patrick Murris
 * @version $Id: VPFSymbolAttributes.java 12680 2009-10-01 03:50:08Z dcollins $
 */
public class VPFSymbolAttributes extends BasicShapeAttributes
{
    public static class LabelAttributes
    {
        private Font font;
        private Color color;
        private Color backgroundColor;
        private double offset;
        private Angle offsetAngle;
        private String prepend;
        private String append;
        private String attributeName;
        private int abbreviationTableId;

        public LabelAttributes()
        {
            this.font = defaultFont;
            this.color = defaultColor;
            this.backgroundColor = defaultBackgroundColor;
        }

        public LabelAttributes(LabelAttributes attributes)
        {
            if (attributes == null)
            {
                String message = Logging.getMessage("nullValue.AttributesIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.font = attributes.getFont();
            this.color = attributes.getColor();
            this.backgroundColor = attributes.getBackgroundColor();
            this.offset = attributes.getOffset();
            this.offsetAngle = attributes.getOffsetAngle();
            this.prepend = attributes.getPrepend();
            this.append = attributes.getAppend();
            this.attributeName = attributes.getAttributeName();
            this.abbreviationTableId = attributes.getAbbreviationTableId();
        }

        public LabelAttributes copy()
        {
            return new LabelAttributes(this);
        }

        public Font getFont()
        {
            return this.font;
        }

        public void setFont(Font font)
        {
            this.font = font;
        }

        public Color getColor()
        {
            return this.color;
        }

        public void setColor(Color color)
        {
            this.color = color;
        }

        public Color getBackgroundColor()
        {
            return this.backgroundColor;
        }

        public void setBackgroundColor(Color color)
        {
            this.backgroundColor = color;
        }

        public double getOffset()
        {
            return offset;
        }

        public void setOffset(double offset)
        {
            this.offset = offset;
        }

        public Angle getOffsetAngle()
        {
            return this.offsetAngle;
        }

        public void setOffsetAngle(Angle angle)
        {
            this.offsetAngle = angle;
        }

        public String getPrepend()
        {
            return this.prepend;
        }

        public void setPrepend(String text)
        {
            this.prepend = text;
        }

        public String getAppend()
        {
            return this.append;
        }

        public void setAppend(String text)
        {
            this.append = text;
        }

        public String getAttributeName()
        {
            return this.attributeName;
        }

        public void setAttributeName(String name)
        {
            this.attributeName = name;
        }

        public int getAbbreviationTableId()
        {
            return this.abbreviationTableId;
        }

        public void setAbbreviationTableId(int tableId)
        {
            this.abbreviationTableId = tableId;
        }
    }

    private static final Font defaultFont = Font.decode("Arial-PLAIN-12");
    private static final Color defaultColor = Color.WHITE;
    private static final Color defaultBackgroundColor = Color.BLACK;

    private VPFFeatureType featureType;
    private VPFSymbolKey symbolKey;
    private Object iconImageSource;
    private double iconImageScale;
    private boolean mipMapIconImage;
    private LabelAttributes[] labelAttributes;
    private double displayPriority;
    private String orientationAttributeName;
    private String description;

    public VPFSymbolAttributes(VPFFeatureType featureType, VPFSymbolKey symbolKey)
    {
        super();
        this.featureType = featureType;
        this.symbolKey = symbolKey;
        this.iconImageSource = null;
        this.iconImageScale = 1d;
        this.mipMapIconImage = true;
        this.labelAttributes = null;
        this.displayPriority = 0;
        this.orientationAttributeName = null;
        this.description = null;
    }

    public VPFSymbolAttributes(VPFSymbolAttributes attributes)
    {
        super(attributes);
        this.featureType = attributes.getFeatureType();
        this.symbolKey = attributes.getSymbolKey();
        this.iconImageSource = attributes.getIconImageSource();
        this.iconImageScale = attributes.getIconImageScale();
        this.mipMapIconImage = attributes.isMipMapIconImage();
        this.displayPriority = attributes.getDisplayPriority();
        this.orientationAttributeName = attributes.getOrientationAttributeName();
        this.description = attributes.getDescription();

        if (attributes.getLabelAttributes() != null)
        {
            LabelAttributes[] array = attributes.getLabelAttributes();
            int numLabelAttributes = array.length;
            this.labelAttributes = new LabelAttributes[numLabelAttributes];

            for (int i = 0; i < numLabelAttributes; i++)
            {
                this.labelAttributes[i] = (array[i] != null) ? array[i].copy() : null;
            }
        }
    }

    public ShapeAttributes copy()
    {
        return new VPFSymbolAttributes(this);
    }

    public VPFFeatureType getFeatureType()
    {
        return this.featureType;
    }

    public VPFSymbolKey getSymbolKey()
    {
        return this.symbolKey;
    }

    public Object getIconImageSource()
    {
        return this.iconImageSource;
    }

    public void setIconImageSource(Object imageSource)
    {
        this.iconImageSource = imageSource;
    }

    public double getIconImageScale()
    {
        return this.iconImageScale;
    }

    public void setIconImageScale(double scale)
    {
        this.iconImageScale = scale;
    }

    public boolean isMipMapIconImage()
    {
        return this.mipMapIconImage;
    }

    public void setMipMapIconImage(boolean mipMap)
    {
        this.mipMapIconImage = mipMap;
    }

    public LabelAttributes[] getLabelAttributes()
    {
        return this.labelAttributes;
    }

    public void setLabelAttributes(LabelAttributes[] attributes)
    {
        this.labelAttributes = attributes;
    }

    public double getDisplayPriority()
    {
        return this.displayPriority;
    }

    public void setDisplayPriority(double displayPriority)
    {
        this.displayPriority = displayPriority;
    }

    public String getOrientationAttributeName()
    {
        return this.orientationAttributeName;
    }

    public void setOrientationAttributeName(String name)
    {
        this.orientationAttributeName = name;
    }

    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }
}
