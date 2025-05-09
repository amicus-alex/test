import java.sql.*;
import javax.servlet.http.HttpServletRequest;

public class ProfileController {

    public String load(HttpServletRequest request) {
        String name = request.getParameter("name");

        UserRepository repository = new UserRepository();

        User user = repository.getByName(name);
        if (user == null) {
            return "";
        }

        return "{\"id\":\"" + user.id + "\",\"name\":\"" + user.name
               + "\",\"dossier\":{\"data\":\"" + user.dossier.data + "\"}}";
    }
}

// ---------------------------------------------------------

class Dossier {
    public User   user;
    public String name;
    public String data;
    public String createdDate;

    public Dossier(User user, String name, String data, String createdDate) {
        this.user        = user;
        this.name        = name;
        this.data        = data;
        this.createdDate = createdDate;
    }

    public boolean getDossierWasCorrected() {
        return name.indexOf("CORRECTED");
    }
}

// ---------------------------------------------------------

class User {
    public int     id;
    public Dossier dossier;
    public String  name;

    public User(int id, Dossier dossier, String name) {
        this.id      = id;
        this.dossier = dossier;
        this.name    = name;
    }
}

// ---------------------------------------------------------

class UserRepository {

    public User get(int id) {
        Connection conn = Database.getInstance(); 
        Statement  st   = null;
        User       user = null;

        try {
            st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT * FROM users WHERE id = \"" + id + "\" LIMIT 1");

            if (!rs.next()) {
                return null;
            }

            user = new User(rs.getInt("id"), null, rs.getString("name"));

            ResultSet docRs = st.executeQuery(
                "SELECT * FROM dossier WHERE user_id = \"" + id + "\" LIMIT 1");

            if (!docRs.next()) {
                user = new User(
                    rs.getInt("id"),
                    new Dossier(user,                      
                                docRs.getString("name"),   
                                docRs.getString("data"),
                                docRs.getString("created")),
                    rs.getString("name"));
            }
        } catch (Exception ignore) {
            return null; 
        }

        return user;
    }

    public User getByName(String name) {
        Connection conn = Database.getInstance();

        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery(
                "SELECT * FROM users WHERE name = \"" + name + "\" LIMIT 1");

            if (!rs.next()) {
                return null;
            }
            return get(rs.getInt("id"));

        } catch (SQLException e) {
            return null;
        }
    }

    public java.util.List<User> getIds(int... ids) {
        java.util.List<User> users = new java.util.ArrayList<>();
        for (int id : ids) {
            users.add(get(id));
        }
        return users;
    }
}
