package dk.glutter.izbrannick.nativesmsforwarder;

public class Sms {

    private String _id;
    private String _thread_id;
	private String _address;
	private String _msg;
    // READ STATE - "0" for have not read sms and "1" for have read sms
	private String _readState;
	private String _time;
	private String _folderName;
    private String _messageType;
    public static final int MESSAGE_TYPE_SMS = 1;
    public static final int MESSAGE_TYPE_MMS = 2;

    public String getId() {
        return _id;
    }
    public String getThreadId() {
        return _thread_id;
    }
    public void setThreadId(String thread_id) {
        _thread_id = thread_id;
    }

	public String getAddress() {
		return _address;
	}

	public String getMsg() {
		return _msg;
	}

	public String getReadState() {
		return _readState;
	}

	public String getTime() {
		return _time;
	}

	public String getFolderName() {
		return _folderName;
	}

	public void setId(String id) {
		_id = id;
	}

	public void setAddress(String address) {
		_address = address;
	}

	public void setMsg(String msg) {
		_msg = msg;
	}

	public void setReadState(String readState) {
		_readState = readState;
	}

	public void setTime(String time) {
		_time = time;
	}

	public void setFolderName(String folderName) {
		_folderName = folderName;
	}

    public String get_messageType() {
        return _messageType;
    }

    public void set_messageType(String _messageType) {
        this._messageType = _messageType;
    }
}
