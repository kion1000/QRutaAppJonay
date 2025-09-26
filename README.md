# 📦 QRuta - Gestión de Repartos y Rutas Óptimas

**QRuta** es una app Android desarrollada como Proyecto Final de DAM (Desarrollo de Aplicaciones Multiplataforma), diseñada para facilitar la vida a repartidores y pequeñas empresas de logística digitalizando la gestión de entregas y rutas.

---

## 🚀 Características principales

- **Registro y autenticación segura** con correo electrónico, Google y verificación por email (Firebase Auth).
- **Escaneo de albaranes** mediante:
  - **QR**: lectura rápida de códigos de los albaranes.
  - **OCR/IA**: reconocimiento de datos desde fotografías de albaranes para extraer información de clientes, direcciones y entregas.
  - **Entrada manual**: para casos puntuales donde no hay QR o imagen disponible.
- **Generación automática de ruta óptima** usando **Google Maps y OpenRouteService**: la app calcula el recorrido más eficiente según las entregas.
- **Gestión de entregas en camino**:
  - Marcar como entregada o posponer.
  - Botón para **llamar al cliente** y **enviar mensaje de aviso de llegada**.
  - Posibilidad de añadir entregas manuales (sin albarán QR).
- **Historial personal de repartos** y seguimiento del estado de cada albarán (planificado para futuras versiones).
- **Sin panel administrativo en móvil**: la app se centra en el repartidor. La gestión administrativa y generación de albaranes con QR se realiza fuera de la app (PC u otro sistema externo).
- **Interfaz moderna, intuitiva y adaptada a móviles**.

---

## 🏆 Sobre el proyecto

QRuta es el trabajo final para el ciclo de Técnico Superior en DAM, con el objetivo de maximizar la eficiencia del **usuario final (repartidor)**:

- Escaneo de QR o OCR y generación automática de la mejor ruta.
- Gestión de incidencias y entregas planeada para futuras versiones.
- Posibilidad de añadir entregas manuales para casos no previstos.

*Nota: Para un uso profesional completo, las empresas deberán incluir el QR en sus albaranes para que el sistema funcione automáticamente.*
*Nota: Este README puede ir cambiando ya que es un proyecto donde se nos pueden ocurrir nuevas mejoras.*

---

## 📲 Tecnologías usadas

- **Android Studio (Java)**
- **Firebase Authentication**
- **Firebase Firestore**
- **API de Google Maps y OpenRouteService** (cálculo de rutas)
- **Tess-Two / OCR (IA)** para reconocimiento de albaranes desde imágenes
- **Material Design**

---

## 📝 Autor

Jonay Armas Suárez  
[jonay_1986@hotmail.com](mailto:jonay_1986@hotmail.com)

---

## 💡 ¿Quieres saber más?

¿Tienes sugerencias o te gustaría colaborar?  
¡Abre un issue en el repositorio o contacta conmigo!
