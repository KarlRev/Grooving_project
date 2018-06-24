package reverends.com.grooving;

public class Groove {
    private  String nome;
    private String autore;
    private String data_di_upload;
    private String genere;
    String strumenti;

    public Groove(String name, String author, String genre,String instruments,String upload_date){
        this.nome=name;
        this.autore=author;
        this.genere=genre;
        this.data_di_upload=upload_date;
        this.strumenti=instruments;
    }

    public String Get_nome(){return nome;}
    public String Get_autore(){return autore;}
    public String Get_data_di_upload(){return data_di_upload;}
    public String Get_genere(){return genere;}
    public String Get_strumenti(){return strumenti;}
}
