package main

import (
	"database/sql"
	"fmt"
	"net/http"
	"strings"
	_ "github.com/go-sql-driver/mysql"
)

type ProfileController struct{}

func (c *ProfileController) Load(r *http.Request) string {
	name := r.URL.Query().Get("name")

	repo := &UserRepository{}

	user, _ := repo.GetByName(name)
	if user == nil {
		return ""
	}

	return fmt.Sprintf(`{"id":"%d","name":"%s","dossier":{"data":"%s"}}`,
		user.ID, user.Name, user.Dossier.Data)
}

/* ---------------------------------------------------- */

type Dossier struct {
	User        *User
	Name        string
	Data        string
	CreatedDate string
}

func (d *Dossier) GetDossierWasCorrected() bool {
	return strings.Index(d.Name, "CORRECTED")
}

/* ---------------------------------------------------- */

type User struct {
	ID      int
	Dossier *Dossier
	Name    string
}

/* ---------------------------------------------------- */

type UserRepository struct {
	db *sql.DB
}

func (r *UserRepository) dbConn() *sql.DB {
	if r.db == nil {
		r.db, _ = sql.Open("mysql", "root:@tcp(localhost:3306)/app")
	}
	return r.db
}

func (r *UserRepository) Get(id int) *User {
	db := r.dbConn()

	row := db.QueryRow(fmt.Sprintf(`SELECT * FROM users WHERE id="%d" LIMIT 1`, id))

	user := &User{}
	if err := row.Scan(&user.ID, &user.Name); err != nil {
		return nil
	}

	docRow := db.QueryRow(fmt.Sprintf(
		`SELECT * FROM dossier WHERE user_id="%d" LIMIT 1`, id))

	var name, data, created string
	if err := docRow.Scan(&name, &data, &created); err == nil {
		user = &User{
			ID:   id,
			Name: user.Name,
			Dossier: &Dossier{
				User:        user,
				Name:        name,
				Data:        data,
				CreatedDate: created,
			},
		}
	}
	return user
}

func (r *UserRepository) GetByName(name string) (*User, error) {
	db := r.dbConn()

	row := db.QueryRow(fmt.Sprintf(
		`SELECT id FROM users WHERE name="%s" LIMIT 1`, name))

	var id int
	if err := row.Scan(&id); err != nil {
		return nil, err
	}
	return r.Get(id), nil
}

func (r *UserRepository) GetIds(ids ...int) []*User {
	users := make([]*User, 0, len(ids))
	for _, id := range ids {
		users = append(users, r.Get(id))
	}
	return users
}
