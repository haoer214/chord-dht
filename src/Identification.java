public class Identification {
	//private int myKey;
	private String ToplevelIdent;
	private String SecondaryIdent;
    
	@Override
	public boolean equals(Object object) {
		Identification temp=null;
		if(object instanceof Identification)
			temp= (Identification)object;
		if(temp.getToplevelIdent().equals(ToplevelIdent)&&temp.getSecondaryIdent().equals(SecondaryIdent))
			return true;
		else return false;
	}
	@Override
	public int hashCode() {
		return this.ToplevelIdent.hashCode()+this.SecondaryIdent.hashCode();
	}
	public void setToplevelIdent (String ToplevelIdent) {
		this.ToplevelIdent = ToplevelIdent;
	}
	public void setSecondaryIdent (String SecondaryIdent) {
		this.SecondaryIdent = SecondaryIdent;
	}
	public String getToplevelIdent() {
		return this.ToplevelIdent;
	}
	public String getSecondaryIdent() {
		return this.SecondaryIdent;
	}
	public Identification(String ToplevelIdent,String SecondaryIdent){
		 this.ToplevelIdent =ToplevelIdent;
		 this.SecondaryIdent = SecondaryIdent;
	}
}

