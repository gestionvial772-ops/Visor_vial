import { Routes } from "@angular/router";

export const CIUDADANO_ROUTES: Routes =[

    {
        path: '',
        redirectTo: 'nuevo-reporte',
        pathMatch: 'full'
    },

    {
        path: 'nuevo-reporte',

        loadComponent: ()=>
            import ('../nuevo-reporte/nuevo-reporte')
                .then(m => m.NuevoReporte)
    },

    {
        path: 'mis-reportes',
        loadComponent: ()=>
            import ('../mis-reportes/mis-reportes')
                .then(m => m.MisReportes)
    },

    {
        path: 'vias',
        loadComponent: ()=>
            import ('../../admin/pages/vias/vias')
                .then(m => m.Vias)
    }
    

]
