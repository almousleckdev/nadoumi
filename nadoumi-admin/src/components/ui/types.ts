/** Shared prop types for the ui/* component library. */

export interface DataTableColumn {
  prop: string
  label: string
  width?: number | string
  minWidth?: number | string
  align?: 'left' | 'center' | 'right'
  sortable?: boolean
  tooltip?: boolean
}

export interface Tab {
  key: string
  label: string
  count?: number
}

export interface DescriptionItem {
  label: string
  value?: string | number | null
  /** named slot to render this row's value instead of plain text */
  slot?: string
}
