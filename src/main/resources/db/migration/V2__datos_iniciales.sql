INSERT INTO rol(nombre,descripcion) VALUES ('ADMIN','Administrador'),('JEFE_COCINA','Jefe de cocina'),('MESERO','Mesero'),('AUXILIAR_COMPRAS','Auxiliar de compras'),('DIRECTOR_COMPRAS','Director de compras'),('DIRECTOR_COMERCIAL','Director comercial');
INSERT INTO sucursal(nombre,direccion) VALUES ('Chapinero','Bogotá'),('Usaquén','Bogotá'),('Teusaquillo','Bogotá');
INSERT INTO categoria(nombre) VALUES ('Proteína'),('Carbohidrato'),('Grano'),('Verdura'),('Fruta');
INSERT INTO unidad(nombre,abreviatura) VALUES ('Kg','kg'),('Libra','lb'),('Litro','l');
INSERT INTO marca(nombre) VALUES ('Premium'),('Campesina');
INSERT INTO producto(nombre,id_categoria,id_unidad) VALUES ('Res',1,1),('Pollo',1,1),('Cerdo',1,1),('Arroz',2,1),('Papa',2,1),('Pasta',2,1),('Frijol',3,1),('Lenteja',3,1),('Repollo',4,1),('Tomate',4,1),('Cebolla',4,1),('Mora',5,1),('Mango',5,1);
INSERT INTO proveedor(nombre,email,telefono) VALUES ('AgroAndes','ventas@agroandes.co','3001112233'),('Frutas del Valle','cotizaciones@frutasvalle.co','3002223344'),('Distribuidora Capital','pedidos@distcapital.co','3003334455');
INSERT INTO inventario(id_sucursal,id_producto,cantidad_actual,cantidad_referencia) SELECT s.id_sucursal,p.id_producto,100,100 FROM sucursal s CROSS JOIN producto p;
UPDATE inventario SET cantidad_actual=20 WHERE id_sucursal=1 AND id_producto=4;
