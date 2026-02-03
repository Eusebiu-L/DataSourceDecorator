public abstract class BaseDataSourceDecorator
        implements DataSource{
    protected DataSource _wrappee;

    protected BaseDataSourceDecorator(DataSource wrappee)
    {
        _wrappee = wrappee;
    }

    @Override
    public abstract void Write(String data);

    @Override
    public abstract String Read();
}
