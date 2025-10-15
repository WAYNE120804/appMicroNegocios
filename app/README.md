# tiendaRopa (MyKiosko) 🛍️
App Android para micro-negocios (tiendas de barrio y emprendimientos de ropa/calzado) que simplifica **inventario, ventas y gastos**, funciona **sin conexión** y permite **respaldar datos en ZIP**.

## ✨ Funcionalidad clave
- **Inventario**: de los prodcutos con precio de la compra y la venta para cualcular tu ganancia de cada prodcuto.
- **Ventas**: registro rápido y notas/observaciones.
- **Gastos**: categorías y filtros por fecha.
- **comprobantes**: la app genera un comprobante de la compra y abonos en PNG para compartirlo con los clientes.
- **Exportación de datos**: genera un **.zip** con **CSV por tabla** (todos los datos).
  > Nota: por ahora **las fotos no se incluyen en el respaldo**; se añadirán en una próxima versión.
- **Seguridad**: PIN/biometría (según hardware).
- **Local-first**: base local (Room/SQLite).

## 🧩 Stack técnico
- Kotlin + Jetpack Compose (Material 3)
- Room (SQLite) • MVVM + Use Cases

## 📸 Capturas

[menu de inicio](docs\inicio.jpg")
[menu_dashboard](docs\inicio_dashboard.jpg")
[menu_dashboard2](docs\inicio_dashboard2.jpg")
[menu_gastos](docs\menuGastos.jpg)
[menu_gastos2](docs\menuGastos2.jpg)
[menu_realizar_venta](docs\menuRealizarVenta.jpg)
[menu_realizar_venta2](docs\menuRealizarVenta2.jpg)!
[menu_realizar_venta3](docs\menuRealizarVenta3.jpg)
[menu_venta_con_abono](docs\ventasConAbono.jpg)
[registrar_abono](docs\registrarAbono.jpg)
[modulo_clientes](docs\moduloClientes.jpg)
[lista_producto](docs\listaProductos.jpg)
[adicionar_producto](docs\adiccionarProducto.jpg)
[adicionar_producto2](docs\adiccionarProducto2.jpg)
[menu_configuracion](docs\menuConfiguracion.jpg)
[historial_abonos_para_compartir_con_clientes](docs\historialAbonosParaCompartir.jpg)


## 🚀 Estado
**MVP funcional**. Próximos pasos: incluir fotos en respaldo ZIP y añadir mas idiomas (por ahora solo en ingles y español) y mas monedas (por ahora solo COP).
## 🔒 Privacidad
Los datos se almacenan en el dispositivo del usuario (no se envían a servidores).

## 👤 Autor
Jhon Sebastian Díaz Villa — Ingeniero en formacion de Sistemas y Telecomunicaciones, U. de Manizales  
Contacto: jhonsebastian-04@hotmail.com / jsdiaz99823@umanizales.edu.co
