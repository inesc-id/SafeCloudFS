package pt.inescid.safecloudfs.cloud;

public interface CloudBroker {



	public void upload(String path, byte[] byteArray);

	public void remove(String path);

	public byte[] download(String path);


//	public void uploadAsync(String path, byte[] byteArray);
//
//	public void uploadSync(String path, byte[] byteArray);






}
