public class ApiDataSource implements  DataSource{
    @Override
    public void Write(String data) {
        System.out.println(data);
    }

    @Override
    public String Read() {
        return "Api Data";
    }
}
