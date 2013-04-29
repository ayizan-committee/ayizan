package ayizan.domain;

public class Identifier
{
    private String id;
    private int attributionId;

    public Identifier(final String id, final int attributionId)
    {
        this.id = id;
        this.attributionId = attributionId;
    }

    public String getId()
    {
        return id;
    }

    public int getAttributionId()
    {
        return attributionId;
    }

    @Override
    public boolean equals(final Object object)
    {
        if(this == object) return true;
        else if(object != null && getClass() == object.getClass()) {
            final Identifier identifier = (Identifier) object;
            return (attributionId == identifier.attributionId && id.equals(identifier.id));
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        int result = id.hashCode();
        result = 31 * result + attributionId;
        return result;
    }

    @Override
    public String toString()
    {
        return "Identifier{" +
                "id='" + id + '\'' +
                ", attributionId=" + attributionId +
                '}';
    }
}
